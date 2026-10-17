using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using MurendeniConstructionArchitects.Api.Dtos;
using MurendeniConstructionArchitects.Data;
using MurendeniConstructionArchitects.Models;

namespace MurendeniConstructionArchitects.Api.Controllers;

[ApiController]
[Route("api/projects")]
[Authorize(Policy = "ApiAnyUser")]
public class ProjectsController : ApiControllerBase
{
    private readonly ApplicationDbContext _db;

    public ProjectsController(ApplicationDbContext db)
    {
        _db = db;
    }

    // GET /api/projects: the projects the caller is allowed to see.
    [HttpGet]
    public async Task<ActionResult<List<ProjectSummaryResponse>>> GetProjects()
    {
        var projects = await VisibleProjects()
            .Include(p => p.Client)
            .Include(p => p.Employee)
            .OrderByDescending(p => p.UpdatedAt)
            .ToListAsync();

        return projects.Select(ToSummary).ToList();
    }

    // GET /api/projects/5: one project with its design files.
    [HttpGet("{id:int}")]
    public async Task<ActionResult<ProjectDetailResponse>> GetProject(int id)
    {
        // The project is only found if it is inside the caller's visible set,
        // so guessing another id returns 404 instead of someone else's data.
        var project = await VisibleProjects()
            .Include(p => p.Client)
            .Include(p => p.Employee)
            .FirstOrDefaultAsync(p => p.ProfileId == id);

        if (project == null)
        {
            return NotFound(new { message = "Project not found." });
        }

        var files = await _db.Designs
            .Where(d => d.ProfileId == id)
            .OrderByDescending(d => d.CreatedAt)
            .ToListAsync();

        return ToDetail(project, files);
    }

    // The same endpoint serves every role; only the visible set changes.
    // Client: own projects. Designer: assigned projects. Sales: all projects.
    private IQueryable<ProjectProfile> VisibleProjects()
    {
        var all = _db.Profiles.AsQueryable();
        int userId = CurrentUserId;

        return CurrentRole switch
        {
            "Client" => all.Where(p => p.ClientId == userId),
            "Designer" => all.Where(p => p.EmployeeId == userId),
            "Sales" => all,
            _ => all.Where(p => false)
        };
    }

    private ProjectSummaryResponse ToSummary(ProjectProfile p)
    {
        return new ProjectSummaryResponse
        {
            ProjectId = p.ProfileId,
            Name = p.ProfileName,
            PropertyType = p.PropertyType.ToString(),
            Status = p.Status.ToString(),
            Location = p.Location,
            CoverImageUrl = ToAbsoluteUrl(p.CoverImageUrl),
            ClientName = p.Client?.FullName,
            DesignerName = p.Employee?.FullName,
            UpdatedAt = AsUtc(p.UpdatedAt)
        };
    }

    private ProjectDetailResponse ToDetail(ProjectProfile p, List<Design> files)
    {
        return new ProjectDetailResponse
        {
            ProjectId = p.ProfileId,
            Name = p.ProfileName,
            PropertyType = p.PropertyType.ToString(),
            Status = p.Status.ToString(),
            Location = p.Location,
            CoverImageUrl = ToAbsoluteUrl(p.CoverImageUrl),
            ClientName = p.Client?.FullName,
            DesignerName = p.Employee?.FullName,
            UpdatedAt = AsUtc(p.UpdatedAt),
            Description = p.Description,
            YearCompleted = p.YearCompleted,
            Files = files.Select(f => new DesignFileResponse
            {
                FileId = f.DesignId,
                Title = f.DesignTitle,
                Status = f.Status,
                FileUrl = ToAbsoluteUrl(f.FileUrl),
                CreatedAt = AsUtc(f.CreatedAt)
            }).ToList()
        };
    }
}