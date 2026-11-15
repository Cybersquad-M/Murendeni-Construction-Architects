using System.ComponentModel.DataAnnotations;
using MurendeniConstructionArchitects.Helpers;

namespace MurendeniConstructionArchitects.Api.Dtos;

// What the app shows on the settings screen.
public class AccountResponse
{
    public string FullName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string? Phone { get; set; }
    public string Role { get; set; } = string.Empty;
}

// Data the app sends to update a client's details.
public class UpdateAccountRequest
{
    [Required, MaxLength(100)]
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

// Data the app sends to change a password.
public class ChangePasswordRequest
{
    [Required]
    public string CurrentPassword { get; set; } = string.Empty;

    [Required, StrongPassword]
    public string NewPassword { get; set; } = string.Empty;
}