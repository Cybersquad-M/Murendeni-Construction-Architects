using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using MurendeniConstructionArchitects.Api.Dtos;
using MurendeniConstructionArchitects.Data;
using MurendeniConstructionArchitects.Models;

namespace MurendeniConstructionArchitects.Api.Controllers;

[ApiController]
[Route("api/inquiries")]
[Authorize(Policy = "ApiClient")]
public class InquiriesController : ApiControllerBase
{
    private readonly ApplicationDbContext _db;

    public InquiriesController(ApplicationDbContext db)
    {
        _db = db;
    }

    // GET /api/inquiries: the logged-in client's own inquiries.
    [HttpGet]
    public async Task<ActionResult<List<InquiryResponse>>> GetMyInquiries()
    {
        int clientId = CurrentUserId;

        var inquiries = await _db.Inquiries
            .Where(i => i.ClientId == clientId)
            .OrderByDescending(i => i.CreatedAt)
            .ToListAsync();

        return inquiries.Select(ToResponse).ToList();
    }

    // POST /api/inquiries: sends a new inquiry from the logged-in client.
    [HttpPost]
    public async Task<ActionResult<InquiryResponse>> Create(CreateInquiryRequest request)
    {
        var client = await _db.Clients.FindAsync(CurrentUserId);
        if (client == null)
        {
            return Unauthorized();
        }

        var inquiry = new Inquiry
        {
            ClientId = client.ClientId,
            Subject = $"App inquiry - {request.Service ?? "General"}",
            ServiceRequested = request.Service,
            Message = request.Message,
            Status = InquiryStatus.Open
        };

        _db.Inquiries.Add(inquiry);
        await _db.SaveChangesAsync();

        return ToResponse(inquiry);
    }

    private InquiryResponse ToResponse(Inquiry i)
    {
        return new InquiryResponse
        {
            InquiryId = i.InquiryId,
            Subject = i.Subject,
            Service = i.ServiceRequested,
            Message = i.Message,
            Status = i.Status.ToString(),
            CreatedAt = AsUtc(i.CreatedAt)
        };
    }
}