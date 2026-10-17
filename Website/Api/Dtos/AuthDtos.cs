using System.ComponentModel.DataAnnotations;

namespace MurendeniConstructionArchitects.Api.Dtos;

// Data the app sends to log in.
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

    [Required, EmailAddress, MaxLength(255)]
    public string Email { get; set; } = string.Empty;

    [MaxLength(20)]
    public string? Phone { get; set; }

    [Required, MinLength(8)]
    public string Password { get; set; } = string.Empty;
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