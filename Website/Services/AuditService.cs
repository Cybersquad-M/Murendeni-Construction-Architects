using System.Security.Claims;
using System.Text.Encodings.Web;
using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using MurendeniConstructionArchitects.Data;
using MurendeniConstructionArchitects.Models;

namespace MurendeniConstructionArchitects.Services;

// Who is doing something, worked out from the current web or API request.
public record AuditActor(
    string Type,        // Admin, Designer, Sales, Client, Anonymous or System
    int? Id,
    string? Name,
    string? Email,
    string Source,      // Website or Mobile app
    string? IpAddress,
    string? UserAgent)
{
    // The person making the current request, read from their login cookie or token.
    public static AuditActor FromHttpContext(HttpContext? context)
    {
        if (context == null)
        {
            return new AuditActor("System", null, "System", null, "System", null, null);
        }

        var user = context.User;

        if (user?.Identity?.IsAuthenticated == true)
        {
            string role = user.FindFirst("role")?.Value ?? "Unknown";
            string? idText = user.FindFirst("uid")?.Value ?? user.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            string? name = user.FindFirst("name")?.Value ?? user.FindFirst(ClaimTypes.Name)?.Value;
            string? email = user.FindFirst("email")?.Value ?? user.FindFirst(ClaimTypes.Email)?.Value;

            return ForUser(context, role, int.TryParse(idText, out int id) ? id : 0, name ?? "", email ?? "");
        }

        return Anonymous(context, null);
    }

    // A known person. Used at login time, when the cookie or token does not exist yet.
    public static AuditActor ForUser(HttpContext? context, string role, int id, string name, string email) =>
        new(role, id == 0 ? null : id, Clip(name, 100), Clip(email, 255), SourceOf(context), IpOf(context), AgentOf(context));

    // Someone who is not signed in. The email is only what they typed, so it is not proof of who they are.
    public static AuditActor Anonymous(HttpContext? context, string? typedEmail) =>
        new("Anonymous", null, "Not signed in", Clip(typedEmail, 255), SourceOf(context), IpOf(context), AgentOf(context));

    private static string SourceOf(HttpContext? c) =>
        c != null && c.Request.Path.StartsWithSegments("/api") ? "Mobile app" : "Website";

    private static string? IpOf(HttpContext? c) => c?.Connection.RemoteIpAddress?.ToString();

    private static string? AgentOf(HttpContext? c) => Clip(c?.Request.Headers.UserAgent.ToString(), 300);

    public static string? Clip(string? value, int max)
    {
        if (string.IsNullOrEmpty(value)) return value;
        return value.Length <= max ? value : value.Substring(0, max);
    }
}

// Writes a log entry for an event that is not a database change, such as a login or a logout.
// (Database changes are logged automatically by AuditSaveChangesInterceptor.)
public class AuditService
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping
    };

    private readonly DbContextOptions<ApplicationDbContext> _options;
    private readonly IHttpContextAccessor _http;
    private readonly ILogger<AuditService> _logger;

    public AuditService(
        DbContextOptions<ApplicationDbContext> options,
        IHttpContextAccessor http,
        ILogger<AuditService> logger)
    {
        _options = options;
        _http = http;
        _logger = logger;
    }

    public async Task LogAsync(
        string action,
        string summary,
        string? entityType = null,
        string? entityId = null,
        object? details = null,
        AuditActor? actor = null)
    {
        try
        {
            actor ??= AuditActor.FromHttpContext(_http.HttpContext);

            // A separate, short-lived database connection, so this never saves
            // anything else that is waiting in the request's main context.
            await using var db = new ApplicationDbContext(_options);

            db.AuditLogs.Add(new AuditLog
            {
                OccurredAt = DateTime.UtcNow,
                ActorType = actor.Type,
                ActorId = actor.Id,
                ActorName = actor.Name,
                ActorEmail = actor.Email,
                Source = actor.Source,
                Action = action,
                EntityType = entityType,
                EntityId = entityId,
                Summary = AuditActor.Clip(summary, 500) ?? string.Empty,
                Details = details == null ? null : JsonSerializer.Serialize(details, JsonOptions),
                IpAddress = actor.IpAddress,
                UserAgent = actor.UserAgent
            });

            await db.SaveChangesAsync();
        }
        catch (Exception ex)
        {
            // A problem with the log should not stop someone from signing in.
            // The error shows up in the Azure log stream instead.
            _logger.LogError(ex, "Could not write audit entry for {Action}", action);
        }
    }
}
