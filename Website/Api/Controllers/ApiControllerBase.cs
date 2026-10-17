using Microsoft.AspNetCore.Mvc;

namespace MurendeniConstructionArchitects.Api.Controllers;

// Shared helpers for the API controllers that need to know who is calling.
public abstract class ApiControllerBase : ControllerBase
{
    // The id stored in the login token. It belongs to a Client or an Employee, depending on the role.
    protected int CurrentUserId => int.Parse(User.FindFirst("uid")!.Value);

    // "Client", "Designer" or "Sales".
    protected string CurrentRole => User.FindFirst("role")?.Value ?? "";

    // Turns "/uploads/my plan.pdf" into "https://site-address/uploads/my%20plan.pdf"
    // so the app can open it directly. Each part of the path is encoded because
    // file names with spaces would otherwise break the link on Android.
    protected string? ToAbsoluteUrl(string? path)
    {
        if (string.IsNullOrEmpty(path)) return null;
        if (path.StartsWith("http", StringComparison.OrdinalIgnoreCase)) return path;

        string encodedPath = string.Join("/", path.Split('/').Select(Uri.EscapeDataString));
        return $"{Request.Scheme}://{Request.Host}{encodedPath}";
    }

    // Dates are stored in UTC. Marking them as UTC makes the JSON end in "Z",
    // so the app converts them to local time correctly.
    protected static DateTime AsUtc(DateTime date)
    {
        return DateTime.SpecifyKind(date, DateTimeKind.Utc);
    }
}