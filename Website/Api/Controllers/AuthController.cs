using Google.Apis.Auth;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using MurendeniConstructionArchitects.Api.Dtos;
using MurendeniConstructionArchitects.Data;
using MurendeniConstructionArchitects.Helpers;
using MurendeniConstructionArchitects.Models;
using MurendeniConstructionArchitects.Services;

namespace MurendeniConstructionArchitects.Api.Controllers;

[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly ApplicationDbContext _db;
    private readonly JwtTokenService _tokens;
    private readonly ExternalLoginService _external;
    private readonly IConfiguration _config;
    private readonly AuditService _audit;

    public AuthController(ApplicationDbContext db, JwtTokenService tokens,
        ExternalLoginService external, IConfiguration config, AuditService audit)
    {
        _db = db;
        _tokens = tokens;
        _external = external;
        _config = config;
        _audit = audit;
    }

    // POST /api/auth/register: creates a client account and returns a login token.
    [HttpPost("register")]
    public async Task<IActionResult> Register(RegisterRequest request)
    {
        bool emailTaken = await _db.Clients.AnyAsync(c => c.Email == request.Email);
        if (emailTaken)
        {
            return Conflict(new { message = "An account with this email already exists." });
        }

        var client = new Client
        {
            FullName = request.FullName,
            Email = request.Email,
            Phone = request.Phone,
            PasswordHash = PasswordHasher.HashPassword(request.Password)
        };

        _db.Clients.Add(client);
        await _db.SaveChangesAsync();

        return Ok(BuildResponse(client.ClientId, client.FullName, client.Email, "Client"));
    }

    // POST /api/auth/login: checks client accounts first, then employee accounts.
    [HttpPost("login")]
    public async Task<IActionResult> Login(LoginRequest request)
    {
        var client = await _db.Clients.FirstOrDefaultAsync(c => c.Email == request.Email);
        if (client != null && client.PasswordHash != null &&
            PasswordHasher.VerifyPassword(request.Password, client.PasswordHash))
        {
            await _audit.LogAsync("Login", $"{client.FullName} signed in", "Client", client.ClientId.ToString(),
                actor: AuditActor.ForUser(HttpContext, "Client", client.ClientId, client.FullName, client.Email));
            return Ok(BuildResponse(client.ClientId, client.FullName, client.Email, "Client"));
        }

        var employee = await _db.Employees.FirstOrDefaultAsync(e => e.Email == request.Email);
        if (employee != null && employee.PasswordHash != null &&
            PasswordHasher.VerifyPassword(request.Password, employee.PasswordHash))
        {
            // Admin accounts stay web-only.
            if (employee.Role == EmployeeRole.Admin)
            {
                await _audit.LogAsync("LoginBlocked", $"{employee.FullName} tried to sign in to the mobile app (admin accounts are website-only)",
                    "Employee", employee.EmployeeId.ToString(),
                    actor: AuditActor.ForUser(HttpContext, "Admin", employee.EmployeeId, employee.FullName, employee.Email));
                return StatusCode(403, new { message = "Admin accounts can only be used on the website." });
            }

            await _audit.LogAsync("Login", $"{employee.FullName} signed in", "Employee", employee.EmployeeId.ToString(),
                actor: AuditActor.ForUser(HttpContext, employee.Role.ToString(), employee.EmployeeId, employee.FullName, employee.Email));

            return Ok(BuildResponse(employee.EmployeeId, employee.FullName, employee.Email, employee.Role.ToString()));
        }

        await _audit.LogAsync("LoginFailed", $"Failed sign-in attempt for {request.Email}",
            actor: AuditActor.Anonymous(HttpContext, request.Email));

        // The same message is returned for a wrong email and a wrong password.
        return Unauthorized(new { message = "Incorrect email or password." });
    }

    // POST /api/auth/google: the app signs in with Google, sends us the Google ID token,
    // and we return the same login response as /login.
    [HttpPost("google")]
    public async Task<IActionResult> Google(GoogleLoginRequest request)
    {
        GoogleJsonWebSignature.Payload payload;
        try
        {
            // Checks the signature, expiry and that the token was issued for OUR Google client.
            payload = await GoogleJsonWebSignature.ValidateAsync(request.IdToken,
                new GoogleJsonWebSignature.ValidationSettings
                {
                    Audience = new[] { _config["Auth:Google:ClientId"] }
                });
        }
        catch (InvalidJwtException)
        {
            await _audit.LogAsync("LoginFailed", "Google sign-in with an invalid token",
                actor: AuditActor.Anonymous(HttpContext, null));
            return Unauthorized(new { message = "Invalid Google sign-in." });
        }

        var user = await _external.ResolveAsync("Google", payload.Subject,
            payload.Email, payload.Name, payload.EmailVerified);

        if (user == null)
        {
            await _audit.LogAsync("LoginFailed", $"Google sign-in refused: no matching account for {payload.Email}",
                actor: AuditActor.Anonymous(HttpContext, payload.Email));
            return Unauthorized(new { message = "We couldn't match that Google account." });
        }

        // Admin accounts stay web-only.
        if (user.Role == "Admin")
        {
            await _audit.LogAsync("LoginBlocked", $"{user.Name} tried to sign in to the mobile app (admin accounts are website-only)",
                "Employee", user.Id.ToString(),
                actor: AuditActor.ForUser(HttpContext, "Admin", user.Id, user.Name, user.Email));
            return StatusCode(403, new { message = "Admin accounts can only be used on the website." });
        }

        await _audit.LogAsync("Login", $"{user.Name} signed in with Google", user.Role == "Client" ? "Client" : "Employee",
            user.Id.ToString(), actor: AuditActor.ForUser(HttpContext, user.Role, user.Id, user.Name, user.Email));

        return Ok(BuildResponse(user.Id, user.Name, user.Email, user.Role));
    }

    // POST /api/auth/logout: the app calls this when the user taps Log out, so the logout
    // is recorded in the system log. The app then deletes its saved token on the phone.
    // The token itself is not cancelled here: it simply expires on its own (Jwt:ExpiryHours).
    [HttpPost("logout")]
    [Authorize(Policy = "ApiAnyUser")]
    public async Task<IActionResult> Logout()
    {
        await _audit.LogAsync("Logout", "Signed out of the mobile app");
        return Ok(new { message = "Signed out." });
    }

    // GET /api/auth/me: returns who the token belongs to. Used to confirm that tokens work.
    [HttpGet("me")]
    [Authorize(Policy = "ApiAnyUser")]
    public IActionResult Me()
    {
        return Ok(new
        {
            userId = User.FindFirst("uid")?.Value,
            name = User.FindFirst("name")?.Value,
            email = User.FindFirst("email")?.Value,
            role = User.FindFirst("role")?.Value
        });
    }

    private AuthResponse BuildResponse(int id, string name, string email, string role)
    {
        return new AuthResponse
        {
            Token = _tokens.CreateToken(id, name, email, role),
            UserId = id,
            FullName = name,
            Email = email,
            Role = role
        };
    }
}