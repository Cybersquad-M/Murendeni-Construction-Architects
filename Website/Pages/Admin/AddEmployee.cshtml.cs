using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using Microsoft.EntityFrameworkCore;
using MurendeniConstructionArchitects.Data;
using MurendeniConstructionArchitects.Helpers;
using MurendeniConstructionArchitects.Models;

namespace MurendeniConstructionArchitects.Pages.Admin;

public class AddEmployeeInput
{
    [Required, MaxLength(100)]
    public string FullName { get; set; } = string.Empty;

    [Required, ValidEmail, MaxLength(255)]
    public string Email
    {
        get => _email;
        set => _email = InputRules.NormalizeEmail(value); // trims and lower-cases
    }

    private string _email = string.Empty;

    public EmployeeRole Role { get; set; } = EmployeeRole.Designer;

    [StrongPassword]
    public string? TemporaryPassword { get; set; }
}

public class AddEmployeeModel : PageModel
{
    private readonly ApplicationDbContext _db;

    public AddEmployeeModel(ApplicationDbContext db)
    {
        _db = db;
    }

    [BindProperty]
    public AddEmployeeInput Input { get; set; } = new();

    public List<Employee> ExistingEmployees { get; set; } = new();
    public string? GeneratedPassword { get; set; }

    public async Task OnGetAsync()
    {
        await LoadExistingAsync();
    }

    public async Task<IActionResult> OnPostAsync()
    {
        if (!ModelState.IsValid)
        {
            await LoadExistingAsync();
            return Page();
        }

        bool emailTaken = await _db.Employees.AnyAsync(e => e.Email == Input.Email);
        if (emailTaken)
        {
            ModelState.AddModelError(nameof(Input.Email), "An employee with this email already exists.");
            await LoadExistingAsync();
            return Page();
        }

        string password = string.IsNullOrWhiteSpace(Input.TemporaryPassword)
            ? "Welcome" + Random.Shared.Next(1000, 9999) + "!"
            : Input.TemporaryPassword;

        var employee = new Employee
        {
            FullName = Input.FullName,
            Email = Input.Email,
            Role = Input.Role,
            PasswordHash = PasswordHasher.HashPassword(password)
        };

        _db.Employees.Add(employee);
        await _db.SaveChangesAsync();

        GeneratedPassword = password;
        Input = new();
        await LoadExistingAsync();
        return Page();
    }

    private async Task LoadExistingAsync()
    {
        ExistingEmployees = await _db.Employees.OrderBy(e => e.FullName).ToListAsync();
    }
}