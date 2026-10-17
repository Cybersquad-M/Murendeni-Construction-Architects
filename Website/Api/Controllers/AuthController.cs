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

    public AuthController(ApplicationDbContext db, JwtTokenService tokens)
    {
        _db = db;
        _tokens = tokens;
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
            return Ok(BuildResponse(client.ClientId, client.FullName, client.Email, "Client"));
        }

        var employee = await _db.Employees.FirstOrDefaultAsync(e => e.Email == request.Email);
        if (employee != null && employee.PasswordHash != null &&
            PasswordHasher.VerifyPassword(request.Password, employee.PasswordHash))
        {
            // Admin accounts stay web-only.
            if (employee.Role == EmployeeRole.Admin)
            {
                return StatusCode(403, new { message = "Admin accounts can only be used on the website." });
            }

            return Ok(BuildResponse(employee.EmployeeId, employee.FullName, employee.Email, employee.Role.ToString()));
        }

        // The same message is returned for a wrong email and a wrong password.
        return Unauthorized(new { message = "Incorrect email or password." });
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