using System.ComponentModel.DataAnnotations;
using MurendeniConstructionArchitects.Helpers;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using Microsoft.EntityFrameworkCore;
using MurendeniConstructionArchitects.Data;
using MurendeniConstructionArchitects.Models;

namespace MurendeniConstructionArchitects.Pages.Admin;

public class EditEmployeeInput
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

    public EmployeeRole Role { get; set; }
}

public class EditEmployeeModel : PageModel
{
    private readonly ApplicationDbContext _db;

    public EditEmployeeModel(ApplicationDbContext db)
    {
        _db = db;
    }

    [BindProperty]
    public EditEmployeeInput Input { get; set; } = new();

    public string? SuccessMessage { get; set; }

    // So the page can warn before someone removes the last Admin account,
    // which would lock everyone out of Add Employee / Manage Portfolio / Reports.
    public int TotalAdminCount { get; set; }
    public bool IsThisTheOnlyAdmin { get; set; }

    public async Task<IActionResult> OnGetAsync(int id)
    {
        var employee = await _db.Employees.FindAsync(id);
        if (employee == null) return NotFound();

        Input.FullName = employee.FullName;
        Input.Email = employee.Email;
        Input.Role = employee.Role;

        await CheckAdminCountAsync(employee);
        return Page();
    }

    public async Task<IActionResult> OnPostAsync(int id)
    {
        var employee = await _db.Employees.FindAsync(id);
        if (employee == null) return NotFound();

        // Email must stay unique - same check pattern as everywhere else.
        bool emailTakenByOther = await _db.Employees
            .AnyAsync(e => e.Email == Input.Email && e.EmployeeId != id);
        if (emailTakenByOther)
        {
            ModelState.AddModelError(nameof(Input.Email), "Another employee already uses this email.");
            await CheckAdminCountAsync(employee);
            return Page();
        }

        // Block the change that would leave zero Admins in the system -
        // otherwise nobody could ever open Add Employee, Manage Portfolio,
        // or Reports again.
        if (employee.Role == EmployeeRole.Admin && Input.Role != EmployeeRole.Admin)
        {
            int adminCount = await _db.Employees.CountAsync(e => e.Role == EmployeeRole.Admin);
            if (adminCount <= 1)
            {
                ModelState.AddModelError(nameof(Input.Role),
                    "This is the only Admin account - promote someone else to Admin first.");
                await CheckAdminCountAsync(employee);
                return Page();
            }
        }

        employee.FullName = Input.FullName;
        employee.Email = Input.Email;
        employee.Role = Input.Role;
        await _db.SaveChangesAsync();

        SuccessMessage = "Employee updated.";
        await CheckAdminCountAsync(employee);
        return Page();
    }

    private async Task CheckAdminCountAsync(Employee employee)
    {
        TotalAdminCount = await _db.Employees.CountAsync(e => e.Role == EmployeeRole.Admin);
        IsThisTheOnlyAdmin = employee.Role == EmployeeRole.Admin && TotalAdminCount <= 1;
    }
}