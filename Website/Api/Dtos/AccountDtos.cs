using System.ComponentModel.DataAnnotations;

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

    [MaxLength(20)]
    public string? Phone { get; set; }
}

// Data the app sends to change a password.
public class ChangePasswordRequest
{
    [Required]
    public string CurrentPassword { get; set; } = string.Empty;

    [Required, MinLength(8)]
    public string NewPassword { get; set; } = string.Empty;
}