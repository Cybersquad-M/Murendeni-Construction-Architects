using System.ComponentModel.DataAnnotations;
using System.Security.Claims;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using Microsoft.EntityFrameworkCore;
using MurendeniConstructionArchitects.Data;
using MurendeniConstructionArchitects.Helpers;
using MurendeniConstructionArchitects.Models;
using MurendeniConstructionArchitects.Services;

namespace MurendeniConstructionArchitects.Pages.Account;

public class LoginInput
{
    [Required, EmailAddress]
    public string Email { get; set; } = string.Empty;

    [Required]
    public string Password { get; set; } = string.Empty;
}

public class LoginModel : PageModel
{
    private readonly ApplicationDbContext _db;
    private readonly AuditService _audit;

    public LoginModel(ApplicationDbContext db, AuditService audit)
    {
        _db = db;
        _audit = audit;
    }

    [BindProperty]
    public LoginInput Input { get; set; } = new();

    public string? ErrorMessage { get; set; }

    public void OnGet()
    {
        // Shows a message if Google sign-in failed (set by ExternalCallback).
        ErrorMessage = TempData["LoginError"] as string;
    }

    // "Continue with Google" button: sends the visitor to Google.
    public IActionResult OnPostGoogle()
    {
        var properties = new AuthenticationProperties
        {
            RedirectUri = Url.Page("/Account/ExternalCallback")
        };
        return Challenge(properties, "Google");
    }

    public async Task<IActionResult> OnPostAsync()
    {
        if (!ModelState.IsValid)
        {
            return Page();
        }

        // Try a Client account first...
        var client = await _db.Clients.FirstOrDefaultAsync(c => c.Email == Input.Email);
        if (client != null && client.PasswordHash != null &&
            PasswordHasher.VerifyPassword(Input.Password, client.PasswordHash))
        {
            await SignInAsClientAsync(client);
            await _audit.LogAsync("Login", $"{client.FullName} signed in", "Client", client.ClientId.ToString(),
                actor: AuditActor.ForUser(HttpContext, "Client", client.ClientId, client.FullName, client.Email));
            return RedirectToPage("/Portal/Index");
        }

        // ...then an Employee account, if no Client matched.
        var employee = await _db.Employees.FirstOrDefaultAsync(e => e.Email == Input.Email);
        if (employee != null && employee.PasswordHash != null &&
            PasswordHasher.VerifyPassword(Input.Password, employee.PasswordHash))
        {
            await SignInAsEmployeeAsync(employee);
            await _audit.LogAsync("Login", $"{employee.FullName} signed in", "Employee", employee.EmployeeId.ToString(),
                actor: AuditActor.ForUser(HttpContext, employee.Role.ToString(), employee.EmployeeId, employee.FullName, employee.Email));
            return RedirectToPage("/Admin/Index");
        }

        // Same message either way - we don't reveal whether the email
        // exists, or which table it belonged to.
        await _audit.LogAsync("LoginFailed", $"Failed sign-in attempt for {Input.Email}",
            actor: AuditActor.Anonymous(HttpContext, Input.Email));

        ErrorMessage = "Incorrect email or password.";
        return Page();
    }

    private async Task SignInAsClientAsync(Client client)
    {
        var claims = new List<Claim>
        {
            new(ClaimTypes.NameIdentifier, client.ClientId.ToString()),
            new(ClaimTypes.Name, client.FullName),
            new(ClaimTypes.Email, client.Email),
            new("role", "Client")
        };
        await SignInAsync(claims);
    }

    private async Task SignInAsEmployeeAsync(Employee employee)
    {
        var claims = new List<Claim>
        {
            new(ClaimTypes.NameIdentifier, employee.EmployeeId.ToString()),
            new(ClaimTypes.Name, employee.FullName),
            new(ClaimTypes.Email, employee.Email),
            new("role", employee.Role.ToString()) // "Admin", "Designer", or "Sales"
        };
        await SignInAsync(claims);
    }

    private async Task SignInAsync(List<Claim> claims)
    {
        var identity = new ClaimsIdentity(claims, CookieAuthenticationDefaults.AuthenticationScheme);
        var principal = new ClaimsPrincipal(identity);
        await HttpContext.SignInAsync(CookieAuthenticationDefaults.AuthenticationScheme, principal);
    }
}