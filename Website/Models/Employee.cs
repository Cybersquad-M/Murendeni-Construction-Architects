using System.ComponentModel.DataAnnotations;

namespace MurendeniConstructionArchitects.Models;

public enum EmployeeRole
{
    Admin,
    Designer,
    Sales
}

public class Employee
{
    public int EmployeeId { get; set; }

    [Required, MaxLength(100)]
    public string FullName { get; set; } = string.Empty;

    [Required, MaxLength(255), EmailAddress]
    public string Email { get; set; } = string.Empty;

    public string? PasswordHash { get; set; }

    public EmployeeRole Role { get; set; }

    // Google sign-in link. The provider's stable user id, never the email.
    [MaxLength(50)]
    public string? ExternalProvider { get; set; }

    [MaxLength(200)]
    public string? ExternalSubject { get; set; }

    public DateTime HiredAt { get; set; } = DateTime.UtcNow;

    public ICollection<Inquiry> Inquiries { get; set; } = new List<Inquiry>();
}