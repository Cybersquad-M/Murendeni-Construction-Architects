using System.ComponentModel.DataAnnotations;
using System.Text.RegularExpressions;

namespace MurendeniConstructionArchitects.Helpers;

// One place for the rules on emails, phone numbers and passwords,
// so the website forms and the mobile API all check the same things.
public static class InputRules
{
    // ---------- Email ----------

    // name@domain.tld : letters/digits/._%+- in the name, dotted domain, and a
    // real ending of 2+ letters (so "a@a" and "a@a.c" are rejected).
    private static readonly Regex EmailPattern = new(
        @"^[A-Za-z0-9](?:[A-Za-z0-9._%+\-]*[A-Za-z0-9])?@(?:[A-Za-z0-9](?:[A-Za-z0-9\-]*[A-Za-z0-9])?\.)+[A-Za-z]{2,}$",
        RegexOptions.Compiled);

    public static string NormalizeEmail(string? value) =>
        (value ?? string.Empty).Trim().ToLowerInvariant();

    public static bool IsValidEmail(string? value)
    {
        var email = NormalizeEmail(value);
        if (email.Length == 0 || email.Length > 254) return false;
        if (email.Contains("..")) return false;

        int at = email.IndexOf('@');
        if (at < 1 || at > 64) return false;      // name part: 1-64 characters

        return EmailPattern.IsMatch(email);
    }

    // ---------- Phone ----------

    // Strips spaces, dashes, dots and brackets. South African numbers written as
    // 0821234567 or 0027821234567 become +27821234567, so every number is stored the same way.
    public static string? NormalizePhone(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) return null;

        var cleaned = Regex.Replace(value.Trim(), @"[\s\-\.\(\)]", "");

        if (cleaned.StartsWith("0027")) cleaned = "+27" + cleaned.Substring(4);
        if (Regex.IsMatch(cleaned, @"^0[1-9][0-9]{8}$")) cleaned = "+27" + cleaned.Substring(1);

        return cleaned;
    }

    // A phone number is optional. If one is given it must be a South African number
    // (+27 and 9 digits) or another international number (+, then 8-15 digits).
    public static bool IsValidPhone(string? value)
    {
        var phone = NormalizePhone(value);
        if (phone == null) return true;

        if (phone.StartsWith("+27")) return Regex.IsMatch(phone, @"^\+27[1-9][0-9]{8}$");
        return Regex.IsMatch(phone, @"^\+[1-9][0-9]{7,14}$");
    }

    // ---------- Password ----------

    private static readonly string[] CommonPasswords =
    {
        "password", "12345678", "123456789", "qwerty", "letmein", "welcome",
        "admin123", "iloveyou", "abc12345", "murendeni"
    };

    // Returns null if the password is fine, otherwise a message saying what is wrong.
    public static string? PasswordProblem(string? password)
    {
        if (string.IsNullOrEmpty(password)) return "Please enter a password.";
        if (password.Length < 8) return "Password must be at least 8 characters.";
        if (password.Length > 100) return "Password must be 100 characters or fewer.";

        var missing = new List<string>();
        if (!password.Any(char.IsUpper)) missing.Add("an uppercase letter");
        if (!password.Any(char.IsLower)) missing.Add("a lowercase letter");
        if (!password.Any(char.IsDigit)) missing.Add("a number");
        if (!password.Any(c => !char.IsLetterOrDigit(c) && !char.IsWhiteSpace(c)))
            missing.Add("a symbol (such as ! @ # $ %)");

        if (missing.Count > 0)
        {
            string list = missing.Count == 1
                ? missing[0]
                : string.Join(", ", missing.Take(missing.Count - 1)) + " and " + missing.Last();
            return "Password must include " + list + ".";
        }

        if (password.Distinct().Count() < 5)
            return "Password is too repetitive. Use a wider mix of characters.";

        var lower = password.ToLowerInvariant();
        if (CommonPasswords.Any(p => lower.Contains(p)))
            return "That password is too common. Please choose something harder to guess.";

        return null;
    }
}

// [ValidEmail] on a property. Empty values are left to [Required].
[AttributeUsage(AttributeTargets.Property | AttributeTargets.Field | AttributeTargets.Parameter)]
public class ValidEmailAttribute : ValidationAttribute
{
    protected override ValidationResult? IsValid(object? value, ValidationContext validationContext)
    {
        if (value is not string text || string.IsNullOrWhiteSpace(text)) return ValidationResult.Success;

        return InputRules.IsValidEmail(text)
            ? ValidationResult.Success
            : new ValidationResult("Please enter a valid email address, like name@example.com.");
    }
}

// [ValidPhone] on a property. The phone number is optional, so empty is fine.
[AttributeUsage(AttributeTargets.Property | AttributeTargets.Field | AttributeTargets.Parameter)]
public class ValidPhoneAttribute : ValidationAttribute
{
    protected override ValidationResult? IsValid(object? value, ValidationContext validationContext)
    {
        if (value is not string text || string.IsNullOrWhiteSpace(text)) return ValidationResult.Success;

        return InputRules.IsValidPhone(text)
            ? ValidationResult.Success
            : new ValidationResult("Please enter a valid phone number, like 082 123 4567 or +27 82 123 4567.");
    }
}

// [StrongPassword] on a NEW password. Do not put it on login fields, or existing users get locked out.
[AttributeUsage(AttributeTargets.Property | AttributeTargets.Field | AttributeTargets.Parameter)]
public class StrongPasswordAttribute : ValidationAttribute
{
    protected override ValidationResult? IsValid(object? value, ValidationContext validationContext)
    {
        if (value is not string text || text.Length == 0) return ValidationResult.Success;

        var problem = InputRules.PasswordProblem(text);
        return problem == null ? ValidationResult.Success : new ValidationResult(problem);
    }
}