using System.ComponentModel.DataAnnotations;

namespace MurendeniConstructionArchitects.Api.Dtos;

// Data sent to change a project's status.
// Allowed values: Inquiry, InProgress, Review, Completed.
public class UpdateProjectStatusRequest
{
    [Required]
    public string Status { get; set; } = string.Empty;
}

// Data sent to upload a design file. The file travels as part of a multipart form.
public class UploadDesignFileRequest
{
    [Required, MaxLength(150)]
    public string Title { get; set; } = string.Empty;

    // Allowed values: draft, review, approved, archived.
    public string Status { get; set; } = "draft";

    [Required]
    public IFormFile File { get; set; } = null!;
}

// An inquiry as shown to sales staff. Works for both registered clients and guests.
public class StaffInquiryResponse
{
    public int InquiryId { get; set; }
    public string ContactName { get; set; } = string.Empty;
    public string? ContactEmail { get; set; }
    public string? ContactPhone { get; set; }
    public string? Service { get; set; }
    public string? Message { get; set; }
    public string Status { get; set; } = string.Empty;      // "Open", "InProgress", "Resolved" or "Closed"
    public int? AssignedToId { get; set; }
    public string? AssignedToName { get; set; }
    public bool HasClientAccount { get; set; }
    public DateTime CreatedAt { get; set; }
}

// Data sent to update an inquiry. A null EmployeeId means "unassigned".
public class UpdateInquiryRequest
{
    [Required]
    public string Status { get; set; } = string.Empty;

    public int? EmployeeId { get; set; }
}

// The result of turning an inquiry into a client account.
public class CreateClientFromInquiryResponse
{
    public int ClientId { get; set; }
    public string Email { get; set; } = string.Empty;
    public bool LinkedExisting { get; set; }

    // Only filled in for brand new accounts. It is shown once and never stored in plain text.
    public string? TemporaryPassword { get; set; }

    public string Message { get; set; } = string.Empty;
}

// A staff member, for the "assign to" lists.
public class EmployeeResponse
{
    public int EmployeeId { get; set; }
    public string FullName { get; set; } = string.Empty;
    public string Role { get; set; } = string.Empty;        // "Designer" or "Sales"
}

// A client, for the "new project" form.
public class ClientListItem
{
    public int ClientId { get; set; }
    public string FullName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string? Phone { get; set; }
}

// Data sent to create a project.
public class CreateProjectRequest
{
    [Range(1, int.MaxValue)]
    public int ClientId { get; set; }

    [Range(1, int.MaxValue)]
    public int EmployeeId { get; set; }

    [Required, MaxLength(150)]
    public string Name { get; set; } = string.Empty;

    [Required]
    public string PropertyType { get; set; } = string.Empty; // "Residential" or "Commercial"

    public string? Description { get; set; }

    [MaxLength(150)]
    public string? Location { get; set; }

    [Range(1900, 2100)]
    public int? YearCompleted { get; set; }
}

// The result of creating a project.
public class CreatedProjectResponse
{
    public int ProjectId { get; set; }
    public string Name { get; set; } = string.Empty;
}