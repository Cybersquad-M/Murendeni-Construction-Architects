using System.ComponentModel.DataAnnotations;
using System.Security.Claims;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using Microsoft.EntityFrameworkCore;
using MurendeniConstructionArchitects.Data;
using MurendeniConstructionArchitects.Helpers;

namespace MurendeniConstructionArchitects.Pages.Admin;

public class AdminChangePasswordInput
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
    public AdminChangePasswordInput Input { get; set; } = new();

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

        int employeeId = int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        var employee = await _db.Employees.FirstAsync(e => e.EmployeeId == employeeId);

        if (employee.PasswordHash == null ||
            !PasswordHasher.VerifyPassword(Input.CurrentPassword, employee.PasswordHash))
        {
            ModelState.AddModelError(nameof(Input.CurrentPassword), "Current password is incorrect.");
            return Page();
        }

        employee.PasswordHash = PasswordHasher.HashPassword(Input.NewPassword);
        await _db.SaveChangesAsync();

        SuccessMessage = "Your password has been changed.";
        Input = new();
        return Page();
    }
}