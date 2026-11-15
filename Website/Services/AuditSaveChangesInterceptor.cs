using System.Runtime.CompilerServices;
using System.Text.Encodings.Web;
using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.ChangeTracking;
using Microsoft.EntityFrameworkCore.Diagnostics;
using MurendeniConstructionArchitects.Models;

namespace MurendeniConstructionArchitects.Services;

// Watches every save to the database and writes a log entry for each row that was
// added, changed or deleted: who did it, when, and the old and new values.
// Because it sits inside the database layer, no page or API call can forget to log.
public class AuditSaveChangesInterceptor : SaveChangesInterceptor
{
    // Never written to the log, not even as a hash. Only "changed" is recorded.
    private static readonly HashSet<string> HiddenFields =
        new(StringComparer.OrdinalIgnoreCase) { "PasswordHash", "ExternalSubject" };

    // Used to name the row in the summary, for example "Updated Client #4 (Jane Dlamini)".
    private static readonly string[] LabelProperties =
        { "FullName", "ProfileName", "DesignTitle", "Subject", "GuestName", "Title", "Name" };

    private static readonly Dictionary<string, string> FriendlyNames = new()
    {
        ["ProjectProfile"] = "Project",
        ["Design"] = "Design file",
        ["Inquiry"] = "Inquiry",
        ["Client"] = "Client",
        ["Employee"] = "Employee",
        ["Payment"] = "Payment"
    };

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping
    };

    private sealed class PendingEntry
    {
        public string EntityType = string.Empty;
        public string Action = string.Empty;       // Created, Updated or Deleted
        public string? EntityId;
        public string? Label;
        public Dictionary<string, object?> Details = new();
        public EntityEntry? AddedEntry;            // new rows only get their id once saved
        public string? KeyName;
        public AuditActor Actor = null!;
    }

    private readonly IHttpContextAccessor _http;
    private readonly ILogger<AuditSaveChangesInterceptor> _logger;
    private readonly ConditionalWeakTable<DbContext, List<PendingEntry>> _pending = new();

    public AuditSaveChangesInterceptor(
        IHttpContextAccessor http,
        ILogger<AuditSaveChangesInterceptor> logger)
    {
        _http = http;
        _logger = logger;
    }

    // ---------- Before the save: note down what is about to change ----------

    public override InterceptionResult<int> SavingChanges(
        DbContextEventData eventData, InterceptionResult<int> result)
    {
        Capture(eventData.Context);
        return result;
    }

    public override ValueTask<InterceptionResult<int>> SavingChangesAsync(
        DbContextEventData eventData, InterceptionResult<int> result,
        CancellationToken cancellationToken = default)
    {
        Capture(eventData.Context);
        return ValueTask.FromResult(result);
    }

    // ---------- After the save: write the log entries ----------

    public override int SavedChanges(SaveChangesCompletedEventData eventData, int result)
    {
        var context = eventData.Context;
        var rows = BuildRows(context);
        if (context != null && rows.Count > 0)
        {
            try
            {
                context.Set<AuditLog>().AddRange(rows);
                context.SaveChanges();
            }
            catch (Exception ex)
            {
                Forget(context, rows, ex);
            }
        }
        return result;
    }

    public override async ValueTask<int> SavedChangesAsync(
        SaveChangesCompletedEventData eventData, int result,
        CancellationToken cancellationToken = default)
    {
        var context = eventData.Context;
        var rows = BuildRows(context);
        if (context != null && rows.Count > 0)
        {
            try
            {
                context.Set<AuditLog>().AddRange(rows);
                await context.SaveChangesAsync(cancellationToken);
            }
            catch (Exception ex)
            {
                Forget(context, rows, ex);
            }
        }
        return result;
    }

    // If the save itself failed, nothing happened, so nothing is logged.
    public override void SaveChangesFailed(DbContextErrorEventData eventData)
    {
        if (eventData.Context != null) _pending.Remove(eventData.Context);
    }

    public override Task SaveChangesFailedAsync(
        DbContextErrorEventData eventData, CancellationToken cancellationToken = default)
    {
        if (eventData.Context != null) _pending.Remove(eventData.Context);
        return Task.CompletedTask;
    }

    // ---------- Work ----------

    private void Capture(DbContext? context)
    {
        if (context == null) return;

        context.ChangeTracker.DetectChanges();

        // The log itself can be added to, but never edited or deleted.
        foreach (var entry in context.ChangeTracker.Entries<AuditLog>())
        {
            if (entry.State == EntityState.Modified || entry.State == EntityState.Deleted)
            {
                throw new InvalidOperationException(
                    "The audit log is read-only. Entries cannot be changed or deleted.");
            }
        }

        var list = _pending.GetOrCreateValue(context);
        list.Clear();

        var actor = AuditActor.FromHttpContext(_http.HttpContext);

        var changed = context.ChangeTracker.Entries()
            .Where(e => e.Entity is not AuditLog &&
                        (e.State == EntityState.Added ||
                         e.State == EntityState.Modified ||
                         e.State == EntityState.Deleted))
            .ToList();

        foreach (var entry in changed)
        {
            var pending = Describe(entry, actor);
            if (pending != null) list.Add(pending);
        }
    }

    private static PendingEntry? Describe(EntityEntry entry, AuditActor actor)
    {
        var pending = new PendingEntry
        {
            EntityType = entry.Metadata.ClrType.Name,
            Actor = actor,
            Label = LabelOf(entry.Entity),
            KeyName = entry.Metadata.FindPrimaryKey()?.Properties.FirstOrDefault()?.Name
        };

        if (entry.State == EntityState.Added)
        {
            pending.Action = "Created";
            pending.AddedEntry = entry;

            foreach (var property in entry.Properties)
            {
                if (property.Metadata.IsPrimaryKey()) continue;

                var value = property.CurrentValue;
                if (value == null) continue;

                pending.Details[property.Metadata.Name] =
                    HiddenFields.Contains(property.Metadata.Name) ? "[set]" : Format(value);
            }
        }
        else if (entry.State == EntityState.Deleted)
        {
            pending.Action = "Deleted";
            pending.EntityId = pending.KeyName == null
                ? null
                : entry.Property(pending.KeyName).OriginalValue?.ToString();

            foreach (var property in entry.Properties)
            {
                if (property.Metadata.IsPrimaryKey()) continue;
                if (HiddenFields.Contains(property.Metadata.Name)) continue;

                var value = property.OriginalValue;
                if (value == null) continue;

                pending.Details[property.Metadata.Name] = Format(value);
            }
        }
        else
        {
            pending.Action = "Updated";
            pending.EntityId = pending.KeyName == null
                ? null
                : entry.Property(pending.KeyName).CurrentValue?.ToString();

            foreach (var property in entry.Properties)
            {
                if (!property.IsModified || property.Metadata.IsPrimaryKey()) continue;
                if (Equals(property.OriginalValue, property.CurrentValue)) continue;

                string name = property.Metadata.Name;

                if (HiddenFields.Contains(name))
                {
                    pending.Details[DisplayField(name)] = "[changed]";
                }
                else
                {
                    pending.Details[name] = new Dictionary<string, object?>
                    {
                        ["old"] = Format(property.OriginalValue),
                        ["new"] = Format(property.CurrentValue)
                    };
                }
            }

            // Nothing really changed, so there is nothing to log.
            if (pending.Details.Count == 0) return null;
        }

        return pending;
    }

    private List<AuditLog> BuildRows(DbContext? context)
    {
        var rows = new List<AuditLog>();
        if (context == null || !_pending.TryGetValue(context, out var list)) return rows;

        _pending.Remove(context);

        foreach (var pending in list)
        {
            string? id = pending.EntityId;
            if (pending.AddedEntry != null && pending.KeyName != null)
            {
                id = pending.AddedEntry.Property(pending.KeyName).CurrentValue?.ToString();
            }

            string kind = FriendlyNames.TryGetValue(pending.EntityType, out var friendly)
                ? friendly
                : pending.EntityType;

            string summary = $"{pending.Action} {kind}";
            if (id != null) summary += $" #{id}";
            if (!string.IsNullOrWhiteSpace(pending.Label)) summary += $" ({pending.Label})";
            if (pending.Action == "Updated")
            {
                summary += ": changed " + string.Join(", ", pending.Details.Keys.Select(DisplayField));
            }

            string details = JsonSerializer.Serialize(pending.Details, JsonOptions);
            if (details.Length > 8000) details = details.Substring(0, 8000);

            rows.Add(new AuditLog
            {
                OccurredAt = DateTime.UtcNow,
                ActorType = pending.Actor.Type,
                ActorId = pending.Actor.Id,
                ActorName = pending.Actor.Name,
                ActorEmail = pending.Actor.Email,
                Source = pending.Actor.Source,
                Action = pending.Action,
                EntityType = pending.EntityType,
                EntityId = id,
                Summary = AuditActor.Clip(summary, 500) ?? string.Empty,
                Details = details,
                IpAddress = pending.Actor.IpAddress,
                UserAgent = pending.Actor.UserAgent
            });
        }

        return rows;
    }

    private void Forget(DbContext context, List<AuditLog> rows, Exception ex)
    {
        // The change itself was already saved. Drop the unsaved log rows so they are
        // not retried by a later save, and record the problem in the Azure log stream.
        foreach (var row in rows)
        {
            var entry = context.Entry(row);
            if (entry.State == EntityState.Added) entry.State = EntityState.Detached;
        }

        _logger.LogError(ex, "Could not write {Count} audit entries", rows.Count);
    }

    private static string DisplayField(string name) => name == "PasswordHash" ? "Password" : name;

    private static string? LabelOf(object entity)
    {
        foreach (var name in LabelProperties)
        {
            var property = entity.GetType().GetProperty(name);
            if (property != null && property.GetValue(entity) is string text && text.Length > 0)
            {
                return text.Length > 60 ? text.Substring(0, 60) : text;
            }
        }
        return null;
    }

    private static object? Format(object? value) => value switch
    {
        null => null,
        string s => s.Length > 300 ? s.Substring(0, 300) + "..." : s,
        DateTime d => d.ToString("yyyy-MM-dd HH:mm:ss"),
        DateTimeOffset d => d.ToString("yyyy-MM-dd HH:mm:ss"),
        bool b => b,
        _ => value.ToString()
    };
}
