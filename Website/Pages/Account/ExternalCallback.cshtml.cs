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

    public ExternalCallbackModel(ExternalLoginService external)
    {
        _external = external;
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