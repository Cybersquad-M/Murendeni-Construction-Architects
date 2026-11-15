using System.Security.Claims;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using MurendeniConstructionArchitects.Services;

namespace MurendeniConstructionArchitects.Pages.Account;

// Google sends the visitor back here after they sign in.
public class ExternalCallbackModel : PageModel
{
    private readonly ExternalLoginService _external;
    private readonly AuditService _audit;

    public ExternalCallbackModel(ExternalLoginService external, AuditService audit)
    {
        _external = external;
        _audit = audit;
    }

    public async Task<IActionResult> OnGetAsync()
    {
        // Read Google's result from the temporary "External" cookie, then discard it.
        var result = await HttpContext.AuthenticateAsync("External");
        await HttpContext.SignOutAsync("External");

        var google = result.Principal;
        var subject = google?.FindFirstValue(ClaimTypes.NameIdentifier);

        if (!result.Succeeded || google == null || subject == null)
        {
            await _audit.LogAsync("LoginFailed", "Google sign-in was cancelled or failed",
                actor: AuditActor.Anonymous(HttpContext, null));
            return Fail("Google sign-in was cancelled or failed. Please try again.");
        }

        var verified = string.Equals(google.FindFirstValue("email_verified"), "true",
            StringComparison.OrdinalIgnoreCase);

        var user = await _external.ResolveAsync(
            "Google",
            subject,
            google.FindFirstValue(ClaimTypes.Email),
            google.FindFirstValue(ClaimTypes.Name),
            verified);

        if (user == null)
        {
            await _audit.LogAsync("LoginFailed",
                $"Google sign-in refused: no matching account for {google.FindFirstValue(ClaimTypes.Email)}",
                actor: AuditActor.Anonymous(HttpContext, google.FindFirstValue(ClaimTypes.Email)));
            return Fail("We couldn't match that Google account. Use an account with a verified email, or sign in with your password.");
        }

        // Same claims as the password login. The role comes from OUR database, not from Google.
        var claims = new List<Claim>
        {
            new(ClaimTypes.NameIdentifier, user.Id.ToString()),
            new(ClaimTypes.Name, user.Name),
            new(ClaimTypes.Email, user.Email),
            new("role", user.Role)
        };

        var identity = new ClaimsIdentity(claims, CookieAuthenticationDefaults.AuthenticationScheme);
        await HttpContext.SignInAsync(
            CookieAuthenticationDefaults.AuthenticationScheme,
            new ClaimsPrincipal(identity));

        await _audit.LogAsync("Login", $"{user.Name} signed in with Google", user.Role == "Client" ? "Client" : "Employee",
            user.Id.ToString(), actor: AuditActor.ForUser(HttpContext, user.Role, user.Id, user.Name, user.Email));

        return user.Role == "Client"
            ? RedirectToPage("/Portal/Index")
            : RedirectToPage("/Admin/Index");
    }

    private IActionResult Fail(string message)
    {
        TempData["LoginError"] = message;
        return RedirectToPage("/Account/Login");
    }
}