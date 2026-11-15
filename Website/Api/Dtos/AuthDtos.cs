using System.ComponentModel.DataAnnotations;
using MurendeniConstructionArchitects.Helpers;

namespace MurendeniConstructionArchitects.Api.Dtos;

// Data the app sends to log in.
// Login is deliberately NOT given the new password rules, so existing accounts can still sign in.
public class LoginRequest
{
    [Required, EmailAddress]
    public string Email { get; set; } = string.Empty;

    [Required]
    public string Password { get; set; } = string.Empty;
}

// Data the app sends to create a new client account.
public class RegisterRequest
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

    [ValidPhone]
    [MaxLength(20)]
    public string? Phone
    {
        get => _phone;
        set => _phone = InputRules.NormalizePhone(value); // tidies spaces, 0821234567 -> +27821234567
    }

    private string? _phone;

    [Required, StrongPassword]
    public string Password { get; set; } = string.Empty;
}

// Data the app sends after the user signs in with Google on the phone.
public class GoogleLoginRequest
{
    [Required]
    public string IdToken { get; set; } = string.Empty;
}

// Data sent back after a successful login or registration.
// The password hash is never included.
public class AuthResponse
{
    public string Token { get; set; } = string.Empty;
    public int UserId { get; set; }
    public string FullName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string Role { get; set; } = string.Empty; // "Client", "Designer" or "Sales"
}