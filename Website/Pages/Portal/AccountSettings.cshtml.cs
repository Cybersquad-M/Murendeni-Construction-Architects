using System.ComponentModel.DataAnnotations;
using MurendeniConstructionArchitects.Helpers;
using System.Security.Claims;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using Microsoft.EntityFrameworkCore;
using MurendeniConstructionArchitects.Data;

namespace MurendeniConstructionArchitects.Pages.Portal;

public class ProfileInput
{
    [Required(ErrorMessage = "Please enter your name.")]
    [MaxLength(100)]
    public string FullName { get; set; } = string.Empty;

    [ValidPhone]
    [MaxLength(20)]
    public string? Phone
    {
        get => _phone;
        set => _phone = InputRules.NormalizePhone(value); // tidies spaces, 0821234567 -> +27821234567
    }

    private string? _phone;
}

public class AccountSettingsModel : PageModel
{
    private readonly ApplicationDbContext _db;

    public AccountSettingsModel(ApplicationDbContext db)
    {
        _db = db;
    }

    [BindProperty]
    public ProfileInput Input { get; set; } = new();

    public string? SuccessMessage { get; set; }

    public async Task OnGetAsync()
    {
        var client = await GetCurrentClientAsync();
        Input.FullName = client.FullName;
        Input.Phone = client.Phone;
    }

    public async Task<IActionResult> OnPostAsync()
    {
        if (!ModelState.IsValid)
        {
            return Page();
        }

        var client = await GetCurrentClientAsync();
        client.FullName = Input.FullName;
        client.Phone = Input.Phone;
        await _db.SaveChangesAsync();

        SuccessMessage = "Your details have been updated.";
        return Page();
    }

    private async Task<Models.Client> GetCurrentClientAsync()
    {
        int clientId = int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        return await _db.Clients.FirstAsync(c => c.ClientId == clientId);
    }
}