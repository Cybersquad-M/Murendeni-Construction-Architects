using Microsoft.EntityFrameworkCore;
using MurendeniConstructionArchitects.Data;
using MurendeniConstructionArchitects.Models;

namespace MurendeniConstructionArchitects.Services;

// Who the Google account turned out to be in OUR database.
public record ExternalUser(int Id, string Name, string Email, string Role);

// Matches a Google account to a Client or Employee row.
// Used by both the website (ExternalCallback) and the mobile API (/api/auth/google).
public class ExternalLoginService
{
    private readonly ApplicationDbContext _db;

    public ExternalLoginService(ApplicationDbContext db)
    {
        _db = db;
    }

    public async Task<ExternalUser?> ResolveAsync(
        string provider, string subject, string? email, string? name, bool emailVerified)
    {
        // 1. Already linked? Match on Google's stable user id, never on email.
        var employee = await _db.Employees.FirstOrDefaultAsync(e =>
            e.ExternalProvider == provider && e.ExternalSubject == subject);
        if (employee != null) return FromEmployee(employee);

        var client = await _db.Clients.FirstOrDefaultAsync(c =>
            c.ExternalProvider == provider && c.ExternalSubject == subject);
        if (client != null) return FromClient(client);

        // 2. First Google sign-in: we may only use the email if Google verified it.
        if (string.IsNullOrWhiteSpace(email) || !emailVerified) return null;

        // Employees are checked first, so a staff email can never end up as a client.
        employee = await _db.Employees.FirstOrDefaultAsync(e => e.Email == email);
        if (employee != null)
        {
            employee.ExternalProvider = provider;
            employee.ExternalSubject = subject;
            await _db.SaveChangesAsync();
            return FromEmployee(employee);
        }

        client = await _db.Clients.FirstOrDefaultAsync(c => c.Email == email);
        if (client == null)
        {
            // New visitors become clients. Employees are NEVER created from Google sign-in.
            client = new Client { FullName = name ?? email, Email = email };
            _db.Clients.Add(client);
        }

        client.ExternalProvider = provider;
        client.ExternalSubject = subject;
        await _db.SaveChangesAsync();
        return FromClient(client);
    }

    private static ExternalUser FromEmployee(Employee e) =>
        new(e.EmployeeId, e.FullName, e.Email, e.Role.ToString());

    private static ExternalUser FromClient(Client c) =>
        new(c.ClientId, c.FullName, c.Email, "Client");
}