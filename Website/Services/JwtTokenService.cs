using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.IdentityModel.Tokens;

namespace MurendeniConstructionArchitects.Services;

// Creates the login tokens (JWTs) that the mobile app sends with every API request.
public class JwtTokenService
{
    private readonly IConfiguration _config;

    public JwtTokenService(IConfiguration config)
    {
        _config = config;
    }

    public string CreateToken(int userId, string name, string email, string role)
    {
        // The claims are the facts stored inside the token.
        var claims = new List<Claim>
        {
            new("uid", userId.ToString()),
            new("name", name),
            new("email", email),
            new("role", role)
        };

        // The secret key signs the token, so any edit to it is detected by the server.
        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_config["Jwt:Key"]!));
        var credentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

        int hours = int.Parse(_config["Jwt:ExpiryHours"] ?? "24");

        var token = new JwtSecurityToken(
            issuer: _config["Jwt:Issuer"],
            audience: _config["Jwt:Audience"],
            claims: claims,
            expires: DateTime.UtcNow.AddHours(hours),
            signingCredentials: credentials);

        return new JwtSecurityTokenHandler().WriteToken(token);
    }
}