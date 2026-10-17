namespace MurendeniConstructionArchitects.Api.Dtos;

// One row in the project list.
public class ProjectSummaryResponse
{
    public int ProjectId { get; set; }
    public string Name { get; set; } = string.Empty;
    public string PropertyType { get; set; } = string.Empty;  // "Residential" or "Commercial"
    public string Status { get; set; } = string.Empty;        // "Inquiry", "InProgress", "Review" or "Completed"
    public string? Location { get; set; }
    public string? CoverImageUrl { get; set; }
    public string? ClientName { get; set; }
    public string? DesignerName { get; set; }
    public DateTime UpdatedAt { get; set; }
}

// One design file inside a project.
public class DesignFileResponse
{
    public int FileId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Status { get; set; } = string.Empty;        // "draft", "review", "approved" or "archived"
    public string? FileUrl { get; set; }
    public DateTime CreatedAt { get; set; }
}

// The full project screen: the summary details plus the description and files.
public class ProjectDetailResponse : ProjectSummaryResponse
{
    public string? Description { get; set; }
    public int? YearCompleted { get; set; }
    public List<DesignFileResponse> Files { get; set; } = new();
}