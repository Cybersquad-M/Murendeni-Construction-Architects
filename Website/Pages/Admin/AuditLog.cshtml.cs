using System.Text.Json;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using Microsoft.EntityFrameworkCore;
using MurendeniConstructionArchitects.Data;
using MurendeniConstructionArchitects.Models;

namespace MurendeniConstructionArchitects.Pages.Admin;

// The system log. Admins only (see Program.cs), and read-only: this page has no
// save, edit or delete handlers, so the log can only be looked at and filtered.
public class AuditLogModel : PageModel
{
    private const int PageSize = 50;

    private readonly ApplicationDbContext _db;

    public AuditLogModel(ApplicationDbContext db)
    {
        _db = db;
    }

    // Filters. SupportsGet means they travel in the address, so a filtered view can be bookmarked or shared.
    [BindProperty(SupportsGet = true)] public DateTime? From { get; set; }
    [BindProperty(SupportsGet = true)] public DateTime? To { get; set; }
    [BindProperty(SupportsGet = true)] public string? Person { get; set; }
    [BindProperty(SupportsGet = true)] public string? Role { get; set; }
    [BindProperty(SupportsGet = true)] public string? ActionName { get; set; }
    [BindProperty(SupportsGet = true)] public string? EntityType { get; set; }
    [BindProperty(SupportsGet = true)] public string? Source { get; set; }
    [BindProperty(SupportsGet = true)] public string? Search { get; set; }
    [BindProperty(SupportsGet = true)] public int PageNumber { get; set; } = 1;

    public List<AuditLog> Entries { get; set; } = new();
    public int TotalCount { get; set; }
    public int TotalPages { get; set; } = 1;

    public List<string> ActionOptions { get; set; } = new();
    public List<string> EntityOptions { get; set; } = new();
    public string[] RoleOptions { get; } = { "Admin", "Designer", "Sales", "Client", "Anonymous", "System" };
    public string[] SourceOptions { get; } = { "Website", "Mobile app" };

    public async Task OnGetAsync()
    {
        var query = _db.AuditLogs.AsNoTracking().AsQueryable();

        // The dates are picked in South African time but stored in UTC.
        if (From.HasValue)
        {
            var start = ToUtc(From.Value.Date);
            query = query.Where(a => a.OccurredAt >= start);
        }
        if (To.HasValue)
        {
            var end = ToUtc(To.Value.Date.AddDays(1));
            query = query.Where(a => a.OccurredAt < end);
        }

        if (!string.IsNullOrWhiteSpace(Person))
        {
            var person = Person.Trim();
            query = query.Where(a =>
                (a.ActorName != null && a.ActorName.Contains(person)) ||
                (a.ActorEmail != null && a.ActorEmail.Contains(person)));
        }
        if (!string.IsNullOrWhiteSpace(Role)) query = query.Where(a => a.ActorType == Role);
        if (!string.IsNullOrWhiteSpace(ActionName)) query = query.Where(a => a.Action == ActionName);
        if (!string.IsNullOrWhiteSpace(EntityType)) query = query.Where(a => a.EntityType == EntityType);
        if (!string.IsNullOrWhiteSpace(Source)) query = query.Where(a => a.Source == Source);

        if (!string.IsNullOrWhiteSpace(Search))
        {
            var text = Search.Trim();
            query = query.Where(a =>
                a.Summary.Contains(text) ||
                (a.Details != null && a.Details.Contains(text)));
        }

        TotalCount = await query.CountAsync();
        TotalPages = Math.Max(1, (int)Math.Ceiling(TotalCount / (double)PageSize));
        PageNumber = Math.Min(Math.Max(1, PageNumber), TotalPages);

        Entries = await query
            .OrderByDescending(a => a.OccurredAt)
            .ThenByDescending(a => a.AuditId)
            .Skip((PageNumber - 1) * PageSize)
            .Take(PageSize)
            .ToListAsync();

        ActionOptions = await _db.AuditLogs.Select(a => a.Action).Distinct().OrderBy(a => a).ToListAsync();
        EntityOptions = await _db.AuditLogs
            .Where(a => a.EntityType != null)
            .Select(a => a.EntityType!)
            .Distinct().OrderBy(a => a).ToListAsync();
    }

    // ---------- Helpers used by the page ----------

    private static readonly TimeZoneInfo LocalZone = FindZone();

    private static TimeZoneInfo FindZone()
    {
        foreach (var id in new[] { "South Africa Standard Time", "Africa/Johannesburg" })
        {
            try { return TimeZoneInfo.FindSystemTimeZoneById(id); }
            catch (TimeZoneNotFoundException) { }
            catch (InvalidTimeZoneException) { }
        }
        return TimeZoneInfo.CreateCustomTimeZone("SAST", TimeSpan.FromHours(2), "SAST", "SAST");
    }

    private static DateTime ToUtc(DateTime local) =>
        TimeZoneInfo.ConvertTimeToUtc(DateTime.SpecifyKind(local, DateTimeKind.Unspecified), LocalZone);

    public static DateTime ToLocal(DateTime utc) =>
        TimeZoneInfo.ConvertTimeFromUtc(DateTime.SpecifyKind(utc, DateTimeKind.Utc), LocalZone);

    // The address of another page of results, keeping every filter.
    public string PageUrl(int page)
    {
        var parts = new List<string> { "PageNumber=" + page };

        void Add(string key, string? value)
        {
            if (!string.IsNullOrWhiteSpace(value)) parts.Add(key + "=" + Uri.EscapeDataString(value));
        }

        Add("From", From?.ToString("yyyy-MM-dd"));
        Add("To", To?.ToString("yyyy-MM-dd"));
        Add("Person", Person);
        Add("Role", Role);
        Add("ActionName", ActionName);
        Add("EntityType", EntityType);
        Add("Source", Source);
        Add("Search", Search);

        return "?" + string.Join("&", parts);
    }

    public static string BadgeFor(string action) => action switch
    {
        "Created" => "badge-resolved",
        "Updated" => "badge-in_progress",
        "Deleted" => "badge-closed",
        "LoginFailed" => "badge-closed",
        "LoginBlocked" => "badge-closed",
        _ => "badge-review"
    };

    // Turns the stored JSON into rows: field, old value, new value.
    public static List<(string Field, string? Old, string? New)> ReadDetails(string? json)
    {
        var rows = new List<(string Field, string? Old, string? New)>();
        if (string.IsNullOrWhiteSpace(json)) return rows;

        try
        {
            using var document = JsonDocument.Parse(json);
            foreach (var property in document.RootElement.EnumerateObject())
            {
                if (property.Value.ValueKind == JsonValueKind.Object &&
                    property.Value.TryGetProperty("new", out var newValue))
                {
                    string oldText = property.Value.TryGetProperty("old", out var oldValue)
                        ? TextOf(oldValue)
                        : "(empty)";
                    rows.Add((property.Name, oldText, TextOf(newValue)));
                }
                else
                {
                    rows.Add((property.Name, null, TextOf(property.Value)));
                }
            }
        }
        catch (JsonException)
        {
            rows.Add(("Details", null, json));
        }

        return rows;
    }

    private static string TextOf(JsonElement element) => element.ValueKind switch
    {
        JsonValueKind.Null => "(empty)",
        JsonValueKind.String => element.GetString() ?? "(empty)",
        _ => element.ToString()
    };
}
