using System.ComponentModel.DataAnnotations;
using System.Security.Claims;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using Microsoft.EntityFrameworkCore;
using MurendeniConstructionArchitects.Data;
using MurendeniConstructionArchitects.Helpers;

namespace MurendeniConstructionArchitects.Pages.Portal;

public class ChangePasswordInput
{
    [Required(ErrorMessage = "Please enter your current password.")]
    public string CurrentPassword { get; set; } = string.Empty;

    [Required(ErrorMessage = "Please enter a new password.")]
    [StrongPassword]
    public string NewPassword { get; set; } = string.Empty;

    [Required]
    [Compare(nameof(NewPassword), ErrorMessage = "Passwords do not match.")]
    public string ConfirmNewPassword { get; set; } = string.Empty;
}

public class ChangePasswordModel : PageModel
{
    private readonly ApplicationDbContext _db;

    public ChangePasswordModel(ApplicationDbContext db)
    {
        _db = db;
    }

    [BindProperty]
    public ChangePasswordInput Input { get; set; } = new();

    public string? SuccessMessage { get; set; }

    public void OnGet()
    {
    }

    public async Task<IActionResult> OnPostAsync()
    {
        if (!ModelState.IsValid)
        {
            return Page();
        }

        int clientId = int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        var client = await _db.Clients.FirstAsync(c => c.ClientId == clientId);

        if (client.PasswordHash == null ||
            !PasswordHasher.VerifyPassword(Input.CurrentPassword, client.PasswordHash))
        {
            ModelState.AddModelError(nameof(Input.CurrentPassword), "Current password is incorrect.");
            return Page();
        }

        client.PasswordHash = PasswordHasher.HashPassword(Input.NewPassword);
        await _db.SaveChangesAsync();

        SuccessMessage = "Your password has been changed.";
        Input = new(); // clear the form after a successful change
        return Page();
    }
}