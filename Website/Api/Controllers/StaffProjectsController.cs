using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using MurendeniConstructionArchitects.Api.Dtos;
using MurendeniConstructionArchitects.Data;
using MurendeniConstructionArchitects.Models;

namespace MurendeniConstructionArchitects.Api.Controllers;

[ApiController]
[Route("api/staff")]
[Authorize(Policy = "ApiSales")]
public class StaffProjectsController : ApiControllerBase
{
    private readonly ApplicationDbContext _db;

    public StaffProjectsController(ApplicationDbContext db)
    {
        _db = db;
    }

    // GET /api/staff/clients: clients to pick from when creating a project.
    [HttpGet("clients")]
    public async Task<ActionResult<List<ClientListItem>>> GetClients()
    {
        return await _db.Clients
            .OrderBy(c => c.FullName)
            .Select(c => new ClientListItem
            {
                ClientId = c.ClientId,
                FullName = c.FullName,
                Email = c.Email,
                Phone = c.Phone
            })
            .ToListAsync();
    }

    // GET /api/staff/employees: designers and sales staff to pick from when assigning work.
    // Admin accounts are left out on purpose, because they are managed on the website only.
    [HttpGet("employees")]
    public async Task<ActionResult<List<EmployeeResponse>>> GetEmployees()
    {
        var employees = await _db.Employees
            .Where(e => e.Role != EmployeeRole.Admin)
            .OrderBy(e => e.FullName)
            .ToListAsync();

        return employees.Select(e => new EmployeeResponse
        {
            EmployeeId = e.EmployeeId,
            FullName = e.FullName,
            Role = e.Role.ToString()
        }).ToList();
    }

    // POST /api/staff/projects: creates a project for a client and assigns a designer.
    [HttpPost("projects")]
    public async Task<ActionResult<CreatedProjectResponse>> CreateProject(CreateProjectRequest request)
    {
        if (!TryParseEnum<PropertyType>(request.PropertyType, out var propertyType))
        {
            return BadRequest(new { message = "Property type must be Residential or Commercial." });
        }

        bool clientExists = await _db.Clients.AnyAsync(c => c.ClientId == request.ClientId);
        if (!clientExists)
        {
            return BadRequest(new { message = "Please choose a valid client." });
        }

        // Only a designer can be assigned to design work.
        var designer = await _db.Employees.FirstOrDefaultAsync(e => e.EmployeeId == request.EmployeeId);
        if (designer == null || designer.Role != EmployeeRole.Designer)
        {
            return BadRequest(new { message = "Please choose a designer for this project." });
        }

        var project = new ProjectProfile
        {
            ClientId = request.ClientId,
            EmployeeId = request.EmployeeId,
            ProfileName = request.Name.Trim(),
            PropertyType = propertyType,
            Description = string.IsNullOrWhiteSpace(request.Description) ? null : request.Description.Trim(),
            Location = string.IsNullOrWhiteSpace(request.Location) ? null : request.Location.Trim(),
            YearCompleted = request.YearCompleted,
            Status = ProjectStatus.Inquiry,
            IsPublished = false // Publishing is decided by an Admin on the website.
        };

        _db.Profiles.Add(project);
        await _db.SaveChangesAsync();

        return new CreatedProjectResponse
        {
            ProjectId = project.ProfileId,
            Name = project.ProfileName
        };
    }
}