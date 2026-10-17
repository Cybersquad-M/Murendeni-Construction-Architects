using System.Text;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using MurendeniConstructionArchitects.Data;
using MurendeniConstructionArchitects.Services;

var builder = WebApplication.CreateBuilder(args);

// NEW (API): reads the secret key that signs login tokens.
var jwtKey = builder.Configuration["Jwt:Key"]
    ?? throw new InvalidOperationException(
        "Jwt:Key is missing. Add it to User Secrets (local) or App Service settings (Azure).");

builder.Services.AddRazorPages(options =>
{
    // Clients can only use /Portal, staff can only use /Admin.
    options.Conventions.AuthorizeFolder("/Portal", "ClientOnly");
    options.Conventions.AuthorizeFolder("/Admin", "StaffOnly");

    // Pages that need a stricter rule than "any staff member".
    options.Conventions.AuthorizePage("/Admin/AddEmployee", "AdminOnly");
    options.Conventions.AuthorizePage("/Admin/ManageEmployees", "AdminOnly");
    options.Conventions.AuthorizePage("/Admin/EditEmployee", "AdminOnly");
    options.Conventions.AuthorizePage("/Admin/ManagePortfolio", "AdminOnly");
    options.Conventions.AuthorizePage("/Admin/Reports", "AdminOnly");

    options.Conventions.AuthorizePage("/Admin/ManageClients", "SalesOrAdmin");
    options.Conventions.AuthorizePage("/Admin/EditClient", "SalesOrAdmin");
    options.Conventions.AuthorizePage("/Admin/ManageProjects", "SalesOrAdmin");
    options.Conventions.AuthorizePage("/Admin/AddProject", "SalesOrAdmin");
    options.Conventions.AuthorizePage("/Admin/EditProject", "SalesOrAdmin");
    options.Conventions.AuthorizePage("/Admin/ManageInquiries", "SalesOrAdmin");
    options.Conventions.AuthorizePage("/Admin/ViewInquiry", "SalesOrAdmin");

    options.Conventions.AuthorizePage("/Admin/MyProjects", "DesignerOrAdmin");
    options.Conventions.AuthorizePage("/Admin/UpdateProjectStatus", "DesignerOrAdmin");
    options.Conventions.AuthorizePage("/Admin/UploadDesignFile", "DesignerOrAdmin");
});

// NEW (API): controllers for the REST API used by the mobile app.
builder.Services.AddControllers();
builder.Services.AddScoped<JwtTokenService>();

// The website uses cookies. NEW (API): the mobile app uses JWT tokens instead.
builder.Services.AddAuthentication(CookieAuthenticationDefaults.AuthenticationScheme)
    .AddCookie(options =>
    {
        options.LoginPath = "/Account/Login";
        // Where a logged-in user is sent after opening an area their role does not allow.
        options.AccessDeniedPath = "/AccessDenied";
    })
    .AddJwtBearer(JwtBearerDefaults.AuthenticationScheme, options =>
    {
        // Keeps claim names exactly as written in the token ("role", "uid", ...).
        options.MapInboundClaims = false;

        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidateAudience = true,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            ValidIssuer = builder.Configuration["Jwt:Issuer"],
            ValidAudience = builder.Configuration["Jwt:Audience"],
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey)),
            NameClaimType = "name",
            RoleClaimType = "role"
        };
    });

// The policies check the "role" claim stored in the cookie or the token.
builder.Services.AddAuthorization(options =>
{
    // Website policies.
    options.AddPolicy("ClientOnly", policy => policy.RequireClaim("role", "Client"));
    options.AddPolicy("StaffOnly", policy => policy.RequireClaim("role", "Admin", "Designer", "Sales"));
    options.AddPolicy("AdminOnly", policy => policy.RequireClaim("role", "Admin"));
    options.AddPolicy("SalesOrAdmin", policy => policy.RequireClaim("role", "Admin", "Sales"));
    options.AddPolicy("DesignerOrAdmin", policy => policy.RequireClaim("role", "Admin", "Designer"));

    // NEW (API): the same idea for API endpoints, using the token instead of the cookie.
    options.AddPolicy("ApiAnyUser", policy => policy
        .AddAuthenticationSchemes(JwtBearerDefaults.AuthenticationScheme)
        .RequireAuthenticatedUser());
    options.AddPolicy("ApiClient", policy => policy
        .AddAuthenticationSchemes(JwtBearerDefaults.AuthenticationScheme)
        .RequireClaim("role", "Client"));
    options.AddPolicy("ApiDesigner", policy => policy
        .AddAuthenticationSchemes(JwtBearerDefaults.AuthenticationScheme)
        .RequireClaim("role", "Designer"));
    options.AddPolicy("ApiSales", policy => policy
        .AddAuthenticationSchemes(JwtBearerDefaults.AuthenticationScheme)
        .RequireClaim("role", "Sales"));
});

// NEW (API): Swagger gives a test page for the API at /swagger.
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(options =>
{
    options.SwaggerDoc("v1", new OpenApiInfo { Title = "Murendeni API", Version = "v1" });

    // Adds an Authorize button so a login token can be pasted in for testing.
    options.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Description = "Paste the token from /api/auth/login (without the word Bearer)."
    });

    options.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        {
            new OpenApiSecurityScheme
            {
                Reference = new OpenApiReference
                {
                    Type = ReferenceType.SecurityScheme,
                    Id = "Bearer"
                }
            },
            Array.Empty<string>()
        }
    });
});

var connectionString = builder.Configuration.GetConnectionString("DefaultConnection");
builder.Services.AddDbContext<ApplicationDbContext>(options =>
    options.UseSqlServer(connectionString, sqlOptions =>
    {
        sqlOptions.EnableRetryOnFailure(
            maxRetryCount: 3,
            maxRetryDelay: TimeSpan.FromSeconds(10),
            errorNumbersToAdd: null);
    }));

var app = builder.Build();

// NEW (API): serves the Swagger test page. Left on in production so the API can be shown in the demo video.
app.UseSwagger();
app.UseSwaggerUI();

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Error");
    app.UseHsts();
}

app.UseHttpsRedirection();
app.UseStaticFiles();
app.UseRouting();

// Authentication works out WHO the caller is; authorization decides WHAT they may open.
app.UseAuthentication();
app.UseAuthorization();

app.MapRazorPages();
app.MapControllers(); // NEW (API)

app.Run();