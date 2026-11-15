using System.ComponentModel.DataAnnotations;
using MurendeniConstructionArchitects.Helpers;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using MurendeniConstructionArchitects.Data;
using MurendeniConstructionArchitects.Models;

namespace MurendeniConstructionArchitects.Pages;

// This is NOT the Inquiry database entity - it's a small, separate class
// that only holds what the <form> on the page actually sends us. Keeping
// this separate from the Inquiry model means the form can have its own
// validation rules (e.g. "Message is required") without changing the
// database model at all.
public class ContactFormInput
{
    [Required(ErrorMessage = "Please enter your name.")]
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

    [MaxLength(100)]
    public string? Service { get; set; }

    [Required(ErrorMessage = "Please enter a message.")]
    public string Message { get; set; } = string.Empty;
}

public class ContactModel : PageModel
{
    private readonly ApplicationDbContext _db;

    public ContactModel(ApplicationDbContext db)
    {
        _db = db;
    }

    // [BindProperty] automatically fills this from the submitted form fields,
    // matching by name (asp-for="Input.FullName" etc. in the .cshtml file).
    [BindProperty]
    public ContactFormInput Input { get; set; } = new();

    // The dropdown list of services shown in the form.
    public List<string> ServiceOptions { get; } = new()
    {
        "Architectural Drafting",
        "2D Floor Plans",
        "3D Building Models",
        "Design Services",
        "Other / Not sure"
    };

    public void OnGet()
    {
    }

    public async Task<IActionResult> OnPostAsync()
    {
        // ModelState checks the [Required]/[EmailAddress] rules above.
        // If anything is missing or invalid, show the form again with errors.
        if (!ModelState.IsValid)
        {
            return Page();
        }

        // Turn the form input into a database record. This is a "guest"
        // inquiry - client_id and employee_id stay empty (null) because the
        // visitor hasn't registered yet and no one has been assigned to
        // follow up yet.
        var inquiry = new Inquiry
        {
            Subject = $"Website enquiry - {Input.Service ?? "General"}",
            GuestName = Input.FullName,
            GuestEmail = Input.Email,
            GuestPhone = Input.Phone,
            ServiceRequested = Input.Service,
            Message = Input.Message,
            Status = InquiryStatus.Open
        };

        _db.Inquiries.Add(inquiry);
        await _db.SaveChangesAsync();

        // TempData survives one redirect - we use it to show a "thank you"
        // message after sending the visitor back to a fresh, empty form.
        // This also stops the message being re-sent if they refresh the page.
        TempData["SuccessMessage"] = "Thanks for reaching out! We'll be in touch shortly.";
        return RedirectToPage();
    }
}