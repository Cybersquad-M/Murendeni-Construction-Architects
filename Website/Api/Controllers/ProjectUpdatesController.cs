using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using MurendeniConstructionArchitects.Api.Dtos;
using MurendeniConstructionArchitects.Data;
using MurendeniConstructionArchitects.Models;

namespace MurendeniConstructionArchitects.Api.Controllers;

[ApiController]
[Route("api/projects")]
public class ProjectUpdatesController : ApiControllerBase
{
    private const long MaxFileBytes = 25 * 1024 * 1024; // 25 MB

    // Only these file types are accepted. Add more extensions here if the team needs them.
    private static readonly string[] AllowedExtensions =
        { ".pdf", ".png", ".jpg", ".jpeg", ".webp", ".dwg", ".dxf", ".rvt", ".skp", ".zip" };

    // Matches the CK_Design_Status rule in the database.
    private static readonly string[] AllowedFileStatuses = { "draft", "review", "approved", "archived" };

    private readonly ApplicationDbContext _db;
    private readonly IWebHostEnvironment _environment;

    public ProjectUpdatesController(ApplicationDbContext db, IWebHostEnvironment environment)
    {
        _db = db;
        _environment = environment;
    }

    // PUT /api/projects/5/status: designers update their own projects, sales staff update any project.
    [HttpPut("{id:int}/status")]
    [Authorize(Policy = "ApiStaff")]
    public async Task<IActionResult> UpdateStatus(int id, UpdateProjectStatusRequest request)
    {
        if (!TryParseEnum<ProjectStatus>(request.Status, out var status))
        {
            return BadRequest(new { message = "Status must be Inquiry, InProgress, Review or Completed." });
        }

        var project = await FindAllowedProjectAsync(id);
        if (project == null)
        {
            return NotFound(new { message = "Project not found." });
        }

        project.Status = status;
        project.UpdatedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();

        return Ok(new { status = project.Status.ToString() });
    }

    // POST /api/projects/5/files: a designer uploads a design file to one of their own projects.
    // The request size limit leaves a small margin above the file limit for the form fields.
    [HttpPost("{id:int}/files")]
    [Authorize(Policy = "ApiDesigner")]
    [Consumes("multipart/form-data")]
    [RequestSizeLimit(MaxFileBytes + 1024 * 1024)]
    public async Task<ActionResult<DesignFileResponse>> UploadFile(int id, [FromForm] UploadDesignFileRequest request)
    {
        var file = request.File;

        if (file.Length == 0)
        {
            return BadRequest(new { message = "The file is empty." });
        }
        if (file.Length > MaxFileBytes)
        {
            return BadRequest(new { message = "The file is too large. The limit is 25 MB." });
        }

        // Path.GetFileName removes any folder parts, so a crafted name cannot write outside the uploads folder.
        string safeName = Path.GetFileName(file.FileName);
        string extension = Path.GetExtension(safeName).ToLowerInvariant();
        if (!AllowedExtensions.Contains(extension))
        {
            return BadRequest(new { message = $"This file type is not allowed. Allowed types: {string.Join(", ", AllowedExtensions)}" });
        }

        string fileStatus = request.Status.Trim().ToLowerInvariant();
        if (!AllowedFileStatuses.Contains(fileStatus))
        {
            return BadRequest(new { message = "File status must be draft, review, approved or archived." });
        }

        var project = await FindAllowedProjectAsync(id);
        if (project == null)
        {
            return NotFound(new { message = "Project not found." });
        }

        // TEMPORARY: saved inside the website's own folder, like the website's upload page.
        // Before going live, this block moves to Azure Blob Storage.
        string uploadsFolder = Path.Combine(_environment.WebRootPath, "uploads");
        Directory.CreateDirectory(uploadsFolder);

        // The random prefix stops two uploads with the same name from overwriting each other.
        string uniqueName = $"{Guid.NewGuid()}_{safeName}";
        string filePath = Path.Combine(uploadsFolder, uniqueName);

        using (var stream = new FileStream(filePath, FileMode.Create))
        {
            await file.CopyToAsync(stream);
        }

        var design = new Design
        {
            ProfileId = id,
            EmployeeId = CurrentUserId,
            DesignTitle = request.Title.Trim(),
            FileUrl = $"/uploads/{uniqueName}",
            Status = fileStatus,
            IsLocked = false
        };

        _db.Designs.Add(design);

        // The project's "last updated" date changes too, so clients see the activity.
        project.UpdatedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();

        return new DesignFileResponse
        {
            FileId = design.DesignId,
            Title = design.DesignTitle,
            Status = design.Status,
            FileUrl = ToAbsoluteUrl(design.FileUrl),
            CreatedAt = AsUtc(design.CreatedAt)
        };
    }

    // Finds a project only if the caller may change it.
    // A designer can only reach projects assigned to them. A sales member can reach any project.
    // A project that is not allowed looks the same as one that does not exist (404).
    private async Task<ProjectProfile?> FindAllowedProjectAsync(int id)
    {
        var project = await _db.Profiles.FindAsync(id);
        if (project == null) return null;

        if (CurrentRole == "Designer" && project.EmployeeId != CurrentUserId)
        {
            return null;
        }

        return project;
    }
}