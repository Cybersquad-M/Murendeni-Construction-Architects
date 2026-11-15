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

namespace MurendeniConstructionArchitects.Pages.Account;

public class RegisterInput
{
    [Required(ErrorMessage = "Please enter your full name.")]
    [MaxLength(100)]
    public string FullName { get; set; } = string.Empty;

    [Required(ErrorMessage = "Please enter your email.")]
    [ValidEmail]
    [MaxLength(255)]
    public string Email
    {
        get => _email;
        set => _email = InputRules.NormalizeEmail(value); // trims and lower-cases
    }

    private string _email = string.Empty;

    [ValidPhone]
    [MaxLength(20)]
    public string? Phone
    {
        get => _phone;
        set => _phone = InputRules.NormalizePhone(value); // tidies spaces, 0821234567 -> +27821234567
    }

    private string? _phone;

    [Required(ErrorMessage = "Please choose a password.")]
    [StrongPassword]
    public string Password { get; set; } = string.Empty;

    [Required]
    [Compare(nameof(Password), ErrorMessage = "Passwords do not match.")]
    public string ConfirmPassword { get; set; } = string.Empty;
}

public class RegisterModel : PageModel
{
    private readonly ApplicationDbContext _db;

    public RegisterModel(ApplicationDbContext db)
    {
        _db = db;
    }

    [BindProperty]
    public RegisterInput Input { get; set; } = new();

    public void OnGet()
    {
    }

    public async Task<IActionResult> OnPostAsync()
    {
        if (!ModelState.IsValid)
        {
            return Page();
        }

        // Emails must be unique - check before we try to insert, so we can
        // show a friendly message instead of a raw database error.
        bool emailTaken = await _db.Clients.AnyAsync(c => c.Email == Input.Email);
        if (emailTaken)
        {
            ModelState.AddModelError(nameof(Input.Email), "An account with this email already exists.");
            return Page();
        }

        var client = new Client
        {
            FullName = Input.FullName,
            Email = Input.Email,
            Phone = Input.Phone,
            PasswordHash = PasswordHasher.HashPassword(Input.Password)
        };

        _db.Clients.Add(client);
        await _db.SaveChangesAsync();

        // Log them straight in after registering, so they land in the portal immediately.
        await SignInClientAsync(client);

        return RedirectToPage("/Portal/Index");
    }

    private async Task SignInClientAsync(Client client)
    {
        // Claims are little facts about the logged-in user, packed into the cookie.
        var claims = new List<Claim>
        {
            new(ClaimTypes.NameIdentifier, client.ClientId.ToString()),
            new(ClaimTypes.Name, client.FullName),
            new(ClaimTypes.Email, client.Email),
            new("role", "Client") // distinguishes clients from employees, once Admin login exists
        };

        var identity = new ClaimsIdentity(claims, CookieAuthenticationDefaults.AuthenticationScheme);
        var principal = new ClaimsPrincipal(identity);

        await HttpContext.SignInAsync(CookieAuthenticationDefaults.AuthenticationScheme, principal);
    }
}