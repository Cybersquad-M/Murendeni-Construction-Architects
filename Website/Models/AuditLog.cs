namespace MurendeniConstructionArchitects.Models;

// One line in the system log: who did what, and when.
// Rows are only ever added. There is no code anywhere that edits or deletes them.
public class AuditLog
{
    public long AuditId { get; set; }

    // Always stored in UTC. The admin page shows it in South African time.
    public DateTime OccurredAt { get; set; } = DateTime.UtcNow;

    public string ActorType { get; set; } = string.Empty;   // Admin, Designer, Sales, Client, Anonymous or System
    public int? ActorId { get; set; }
    public string? ActorName { get; set; }
    public string? ActorEmail { get; set; }

    public string Source { get; set; } = string.Empty;      // Website or Mobile app
    public string Action { get; set; } = string.Empty;      // Login, LoginFailed, Logout, Created, Updated, Deleted

    public string? EntityType { get; set; }
    public string? EntityId { get; set; }

    public string Summary { get; set; } = string.Empty;
    public string? Details { get; set; }                    // JSON with the old and new values

    public string? IpAddress { get; set; }
    public string? UserAgent { get; set; }
}
