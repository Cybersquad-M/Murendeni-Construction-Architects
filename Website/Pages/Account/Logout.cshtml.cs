using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using MurendeniConstructionArchitects.Services;

namespace MurendeniConstructionArchitects.Pages.Account;

public class LogoutModel : PageModel
{
    private readonly AuditService _audit;

    public LogoutModel(AuditService audit)
    {
        _audit = audit;
    }

    public async Task<IActionResult> OnGetAsync()
    {
        // Logged before the cookie is deleted, so we still know who it was.
        if (User.Identity?.IsAuthenticated == true)
        {
            await _audit.LogAsync("Logout", "Signed out");
        }

        await HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
        await HttpContext.SignOutAsync("External");
        return RedirectToPage("/Index");
    }
}