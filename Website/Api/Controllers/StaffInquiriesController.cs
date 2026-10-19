using System.Security.Cryptography;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using MurendeniConstructionArchitects.Api.Dtos;
using MurendeniConstructionArchitects.Data;
using MurendeniConstructionArchitects.Helpers;
using MurendeniConstructionArchitects.Models;

namespace MurendeniConstructionArchitects.Api.Controllers;

[ApiController]
[Route("api/staff/inquiries")]
[Authorize(Policy = "ApiSales")]
public class StaffInquiriesController : ApiControllerBase
{
    private readonly ApplicationDbContext _db;

    public StaffInquiriesController(ApplicationDbContext db)
    {
        _db = db;
    }

    // GET /api/staff/inquiries?status=Open: every inquiry, newest first. The status filter is optional.
    [HttpGet]
    public async Task<ActionResult<List<StaffInquiryResponse>>> GetAll([FromQuery] string? status)
    {
        var query = _db.Inquiries
            .Include(i => i.Client)
            .Include(i => i.Employee)
            .AsQueryable();

        if (!string.IsNullOrWhiteSpace(status))
        {
            if (!TryParseEnum<InquiryStatus>(status, out var parsed))
            {
                return BadRequest(new { message = "Status must be Open, InProgress, Resolved or Closed." });
            }
            query = query.Where(i => i.Status == parsed);
        }

        var inquiries = await query.OrderByDescending(i => i.CreatedAt).ToListAsync();
        return inquiries.Select(ToResponse).ToList();
    }

    // GET /api/staff/inquiries/5: one inquiry.
    [HttpGet("{id:int}")]
    public async Task<ActionResult<StaffInquiryResponse>> GetOne(int id)
    {
        var inquiry = await LoadAsync(id);
        if (inquiry == null)
        {
            return NotFound(new { message = "Inquiry not found." });
        }

        return ToResponse(inquiry);
    }

    // PUT /api/staff/inquiries/5: changes the status and who the inquiry is assigned to.
    [HttpPut("{id:int}")]
    public async Task<ActionResult<StaffInquiryResponse>> Update(int id, UpdateInquiryRequest request)
    {
        if (!TryParseEnum<InquiryStatus>(request.Status, out var status))
        {
            return BadRequest(new { message = "Status must be Open, InProgress, Resolved or Closed." });
        }

        var inquiry = await _db.Inquiries.FirstOrDefaultAsync(i => i.InquiryId == id);
        if (inquiry == null)
        {
            return NotFound(new { message = "Inquiry not found." });
        }

        if (request.EmployeeId != null)
        {
            int employeeId = request.EmployeeId.Value;
            bool employeeExists = await _db.Employees.AnyAsync(e => e.EmployeeId == employeeId);
            if (!employeeExists)
            {
                return BadRequest(new { message = "That employee does not exist." });
            }
        }

        inquiry.Status = status;
        inquiry.EmployeeId = request.EmployeeId;
        await _db.SaveChangesAsync();

        var updated = await LoadAsync(id);
        return ToResponse(updated!);
    }

    // POST /api/staff/inquiries/5/create-client: turns a guest inquiry into a client account,
    // using the name, email and phone that the visitor typed into the contact form.
    [HttpPost("{id:int}/create-client")]
    public async Task<ActionResult<CreateClientFromInquiryResponse>> CreateClient(int id)
    {
        var inquiry = await _db.Inquiries.FirstOrDefaultAsync(i => i.InquiryId == id);
        if (inquiry == null)
        {
            return NotFound(new { message = "Inquiry not found." });
        }

        if (inquiry.ClientId != null)
        {
            return BadRequest(new { message = "This inquiry is already linked to a client account." });
        }

        if (string.IsNullOrWhiteSpace(inquiry.GuestEmail))
        {
            return BadRequest(new { message = "This inquiry has no email address, so an account cannot be created." });
        }

        // If the person already registered on their own, the inquiry is linked to that account
        // instead of creating a duplicate.
        var existing = await _db.Clients.FirstOrDefaultAsync(c => c.Email == inquiry.GuestEmail);
        if (existing != null)
        {
            inquiry.ClientId = existing.ClientId;
            await _db.SaveChangesAsync();

            return new CreateClientFromInquiryResponse
            {
                ClientId = existing.ClientId,
                Email = existing.Email,
                LinkedExisting = true,
                Message = "Linked to the existing account for that email."
            };
        }

        string password = GenerateTemporaryPassword();

        var client = new Client
        {
            FullName = inquiry.GuestName ?? "New Client",
            Email = inquiry.GuestEmail,
            Phone = inquiry.GuestPhone,
            PasswordHash = PasswordHasher.HashPassword(password)
        };

        _db.Clients.Add(client);
        await _db.SaveChangesAsync(); // Saved first, so the new client gets its id.

        inquiry.ClientId = client.ClientId;
        await _db.SaveChangesAsync();

        return new CreateClientFromInquiryResponse
        {
            ClientId = client.ClientId,
            Email = client.Email,
            LinkedExisting = false,
            TemporaryPassword = password,
            Message = "Account created. Give the temporary password to the client, who can change it in Settings."
        };
    }

    private async Task<Inquiry?> LoadAsync(int id)
    {
        return await _db.Inquiries
            .Include(i => i.Client)
            .Include(i => i.Employee)
            .FirstOrDefaultAsync(i => i.InquiryId == id);
    }

    private StaffInquiryResponse ToResponse(Inquiry i) => new()
    {
        InquiryId = i.InquiryId,
        ContactName = i.Client?.FullName ?? i.GuestName ?? "Unknown",
        ContactEmail = i.Client?.Email ?? i.GuestEmail,
        ContactPhone = i.Client?.Phone ?? i.GuestPhone,
        Service = i.ServiceRequested,
        Message = i.Message,
        Status = i.Status.ToString(),
        AssignedToId = i.EmployeeId,
        AssignedToName = i.Employee?.FullName,
        HasClientAccount = i.ClientId != null,
        CreatedAt = AsUtc(i.CreatedAt)
    };

    // Builds a random temporary password from a secure random source.
    // Look-alike characters (0/O, 1/l/I) are left out, so it is easy to read out loud.
    private static string GenerateTemporaryPassword()
    {
        const string characters = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

        var result = new char[10];
        for (int i = 0; i < result.Length; i++)
        {
            result[i] = characters[RandomNumberGenerator.GetInt32(characters.Length)];
        }

        // The ending guarantees at least one symbol.
        return new string(result) + "!";
    }
}