using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MurendeniConstructionArchitects.Api.Dtos;
using MurendeniConstructionArchitects.Data;
using MurendeniConstructionArchitects.Helpers;

namespace MurendeniConstructionArchitects.Api.Controllers;

[ApiController]
[Route("api/account")]
[Authorize(Policy = "ApiAnyUser")]
public class AccountController : ApiControllerBase
{
    private readonly ApplicationDbContext _db;

    public AccountController(ApplicationDbContext db)
    {
        _db = db;
    }

    // GET /api/account: details for the settings screen. Works for clients and staff.
    [HttpGet]
    public async Task<ActionResult<AccountResponse>> GetAccount()
    {
        if (CurrentRole == "Client")
        {
            var client = await _db.Clients.FindAsync(CurrentUserId);
            if (client == null) return NotFound();

            return new AccountResponse
            {
                FullName = client.FullName,
                Email = client.Email,
                Phone = client.Phone,
                Role = "Client"
            };
        }

        var employee = await _db.Employees.FindAsync(CurrentUserId);
        if (employee == null) return NotFound();

        return new AccountResponse
        {
            FullName = employee.FullName,
            Email = employee.Email,
            Role = employee.Role.ToString()
        };
    }

    // PUT /api/account: clients update their own name and phone.
    // Staff details are changed by an Admin on the website.
    [HttpPut]
    [Authorize(Policy = "ApiClient")]
    public async Task<ActionResult<AccountResponse>> UpdateAccount(UpdateAccountRequest request)
    {
        var client = await _db.Clients.FindAsync(CurrentUserId);
        if (client == null) return NotFound();

        client.FullName = request.FullName;
        client.Phone = request.Phone;
        await _db.SaveChangesAsync();

        return new AccountResponse
        {
            FullName = client.FullName,
            Email = client.Email,
            Phone = client.Phone,
            Role = "Client"
        };
    }

    // POST /api/account/change-password: needs the current password first.
    // A wrong current password returns 400, not 401, because the app treats
    // 401 as "login expired" and sends the user back to the login screen.
    [HttpPost("change-password")]
    public async Task<IActionResult> ChangePassword(ChangePasswordRequest request)
    {
        if (CurrentRole == "Client")
        {
            var client = await _db.Clients.FindAsync(CurrentUserId);
            if (client == null) return NotFound();

            if (client.PasswordHash == null ||
                !PasswordHasher.VerifyPassword(request.CurrentPassword, client.PasswordHash))
            {
                return BadRequest(new { message = "Current password is incorrect." });
            }

            client.PasswordHash = PasswordHasher.HashPassword(request.NewPassword);
        }
        else
        {
            var employee = await _db.Employees.FindAsync(CurrentUserId);
            if (employee == null) return NotFound();

            if (employee.PasswordHash == null ||
                !PasswordHasher.VerifyPassword(request.CurrentPassword, employee.PasswordHash))
            {
                return BadRequest(new { message = "Current password is incorrect." });
            }

            employee.PasswordHash = PasswordHasher.HashPassword(request.NewPassword);
        }

        await _db.SaveChangesAsync();
        return Ok(new { message = "Password changed." });
    }
}