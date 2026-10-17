using System.ComponentModel.DataAnnotations;

namespace MurendeniConstructionArchitects.Api.Dtos;

// Data the app sends to create an inquiry.
public class CreateInquiryRequest
{
    [MaxLength(100)]
    public string? Service { get; set; }

    [Required]
    public string Message { get; set; } = string.Empty;
}

// An inquiry as shown in the app.
public class InquiryResponse
{
    public int InquiryId { get; set; }
    public string Subject { get; set; } = string.Empty;
    public string? Service { get; set; }
    public string? Message { get; set; }
    public string Status { get; set; } = string.Empty;        // "Open", "InProgress", "Resolved" or "Closed"
    public DateTime CreatedAt { get; set; }
}