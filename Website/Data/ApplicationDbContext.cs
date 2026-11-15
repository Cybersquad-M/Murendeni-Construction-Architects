using Microsoft.EntityFrameworkCore;
using MurendeniConstructionArchitects.Models;

namespace MurendeniConstructionArchitects.Data;

public class ApplicationDbContext : DbContext
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
        : base(options) { }

    public DbSet<Client> Clients => Set<Client>();
    public DbSet<Employee> Employees => Set<Employee>();
    public DbSet<ProjectProfile> Profiles => Set<ProjectProfile>();
    public DbSet<Design> Designs => Set<Design>();
    public DbSet<Payment> Payments => Set<Payment>();
    public DbSet<Inquiry> Inquiries => Set<Inquiry>();
    public DbSet<AuditLog> AuditLogs => Set<AuditLog>();
    private static string ProjectStatusToDb(ProjectStatus status) => status switch
    {
        ProjectStatus.Inquiry => "inquiry",
        ProjectStatus.InProgress => "in_progress",
        ProjectStatus.Review => "review",
        ProjectStatus.Completed => "completed",
        _ => "inquiry"
    };



    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        // The system log. Rows are only ever added, never changed (the database also enforces this).
        modelBuilder.Entity<AuditLog>(e =>
        {
            e.ToTable("AuditLog");
            e.HasKey(x => x.AuditId);
            e.Property(x => x.AuditId).HasColumnName("audit_id").ValueGeneratedOnAdd();
            e.Property(x => x.OccurredAt).HasColumnName("occurred_at");
            e.Property(x => x.ActorType).HasColumnName("actor_type").HasMaxLength(20);
            e.Property(x => x.ActorId).HasColumnName("actor_id");
            e.Property(x => x.ActorName).HasColumnName("actor_name").HasMaxLength(100);
            e.Property(x => x.ActorEmail).HasColumnName("actor_email").HasMaxLength(255);
            e.Property(x => x.Source).HasColumnName("source").HasMaxLength(20);
            e.Property(x => x.Action).HasColumnName("action").HasMaxLength(30);
            e.Property(x => x.EntityType).HasColumnName("entity_type").HasMaxLength(50);
            e.Property(x => x.EntityId).HasColumnName("entity_id").HasMaxLength(50);
            e.Property(x => x.Summary).HasColumnName("summary").HasMaxLength(500);
            e.Property(x => x.Details).HasColumnName("details");
            e.Property(x => x.IpAddress).HasColumnName("ip_address").HasMaxLength(45);
            e.Property(x => x.UserAgent).HasColumnName("user_agent").HasMaxLength(300);
        });

        modelBuilder.Entity<Client>(e =>
        {
            e.ToTable("Client");
            e.HasKey(x => x.ClientId);
            e.Property(x => x.ClientId).HasColumnName("client_id");
            e.Property(x => x.FullName).HasColumnName("fullname");
            e.Property(x => x.Email).HasColumnName("email");
            e.Property(x => x.Phone).HasColumnName("phone");
            e.Property(x => x.PasswordHash).HasColumnName("password_hash");
            e.Property(x => x.CreatedAt).HasColumnName("created_at");
            e.Property(x => x.ExternalProvider).HasColumnName("external_provider");
            e.Property(x => x.ExternalSubject).HasColumnName("external_subject");
            e.HasIndex(x => x.Email).IsUnique();
        });

        modelBuilder.Entity<Employee>(e =>
        {
            e.ToTable("Employee");
            e.HasKey(x => x.EmployeeId);
            e.Property(x => x.EmployeeId).HasColumnName("employee_id");
            e.Property(x => x.FullName).HasColumnName("fullname");
            e.Property(x => x.Email).HasColumnName("email");
            e.Property(x => x.PasswordHash).HasColumnName("password_hash");
            e.Property(x => x.Role).HasColumnName("role")
                .HasConversion<string>().HasMaxLength(20);
            e.Property(x => x.HiredAt).HasColumnName("hired_at");
            e.Property(x => x.ExternalProvider).HasColumnName("external_provider");
            e.Property(x => x.ExternalSubject).HasColumnName("external_subject");
            e.HasIndex(x => x.Email).IsUnique();
        });

        modelBuilder.Entity<ProjectProfile>(e =>
        {
            e.HasKey(x => x.ProfileId);
            e.Property(x => x.PropertyType).HasConversion<string>().HasMaxLength(20);
            e.Property(x => x.Status)
    .HasConversion(
        status => ProjectStatusToDb(status),
        value => ProjectStatusFromDb(value))
    .HasMaxLength(20);
            e.Property(x => x.Category).HasConversion<string>().HasMaxLength(20)
                .HasColumnName("project_category");
            e.Property(x => x.IsFeatured).HasColumnName("is_featured");
            e.HasOne(x => x.Client).WithMany()
                .HasForeignKey(x => x.ClientId).OnDelete(DeleteBehavior.Restrict);
            e.HasOne(x => x.Employee).WithMany()
                .HasForeignKey(x => x.EmployeeId).OnDelete(DeleteBehavior.Restrict);

        });


        modelBuilder.Entity<Design>(e =>
        {
            e.ToTable("Design");
            e.HasKey(x => x.DesignId);
            e.Property(x => x.DesignId).HasColumnName("design_id");
            e.Property(x => x.ProfileId).HasColumnName("profile_id");
            e.Property(x => x.EmployeeId).HasColumnName("employee_id");
            e.Property(x => x.DesignTitle).HasColumnName("design_title");
            e.Property(x => x.FileUrl).HasColumnName("file_url");
            e.Property(x => x.IsLocked).HasColumnName("is_locked");
            e.Property(x => x.Status).HasColumnName("status");
            e.Property(x => x.CreatedAt).HasColumnName("created_at");
            e.HasOne(x => x.Profile).WithMany(p => p.Designs)
                .HasForeignKey(x => x.ProfileId).OnDelete(DeleteBehavior.Restrict);
            e.HasOne(x => x.Employee).WithMany()
                .HasForeignKey(x => x.EmployeeId).OnDelete(DeleteBehavior.Restrict);
        });

        modelBuilder.Entity<Payment>(e =>
        {
            e.ToTable("Payment");
            e.HasKey(x => x.PaymentId);
            e.Property(x => x.PaymentId).HasColumnName("payment_id");
            e.Property(x => x.ClientId).HasColumnName("client_id");
            e.Property(x => x.ProfileId).HasColumnName("profile_id");
            e.Property(x => x.DesignId).HasColumnName("design_id");
            e.Property(x => x.Amount).HasColumnName("amount").HasColumnType("decimal(10,2)");
            e.Property(x => x.Status).HasColumnName("status")
                .HasConversion<string>().HasMaxLength(20);
            e.Property(x => x.PaidAt).HasColumnName("paid_at");
            e.HasOne(x => x.Client).WithMany(c => c.Payments)
                .HasForeignKey(x => x.ClientId).OnDelete(DeleteBehavior.Restrict);
            e.HasOne(x => x.Profile).WithMany(p => p.Payments)
                .HasForeignKey(x => x.ProfileId).OnDelete(DeleteBehavior.Restrict);
            e.HasOne(x => x.Design).WithMany()
                .HasForeignKey(x => x.DesignId).OnDelete(DeleteBehavior.Restrict);
        });

        modelBuilder.Entity<Inquiry>(e =>
        {
            e.ToTable("Inquiry");
            e.HasKey(x => x.InquiryId);
            e.Property(x => x.InquiryId).HasColumnName("inquiry_id");
            e.Property(x => x.ClientId).HasColumnName("client_id");
            e.Property(x => x.EmployeeId).HasColumnName("employee_id");
            e.Property(x => x.Subject).HasColumnName("subject");
            e.Property(x => x.Status)
    .HasConversion(
        status => InquiryStatusToDb(status),
        value => InquiryStatusFromDb(value))
    .HasMaxLength(20);
            e.Property(x => x.GuestName).HasColumnName("guest_name");
            e.Property(x => x.GuestEmail).HasColumnName("guest_email");
            e.Property(x => x.GuestPhone).HasColumnName("guest_phone");
            e.Property(x => x.ServiceRequested).HasColumnName("service_requested");
            e.Property(x => x.Message).HasColumnName("message");
            e.Property(x => x.CreatedAt).HasColumnName("created_at");
            e.HasOne(x => x.Client).WithMany(c => c.Inquiries)
                .HasForeignKey(x => x.ClientId).OnDelete(DeleteBehavior.SetNull);
            e.HasOne(x => x.Employee).WithMany(emp => emp.Inquiries)
                .HasForeignKey(x => x.EmployeeId).OnDelete(DeleteBehavior.SetNull);
        });
    }
    private static ProjectStatus ProjectStatusFromDb(string value) => value switch
    {
        "inquiry" => ProjectStatus.Inquiry,
        "in_progress" => ProjectStatus.InProgress,
        "review" => ProjectStatus.Review,
        "completed" => ProjectStatus.Completed,
        _ => ProjectStatus.Inquiry
    };
    private static string InquiryStatusToDb(InquiryStatus status) => status switch
    {
        InquiryStatus.Open => "open",
        InquiryStatus.InProgress => "in_progress",
        InquiryStatus.Resolved => "resolved",
        InquiryStatus.Closed => "closed",
        _ => "open"
    };

    private static InquiryStatus InquiryStatusFromDb(string value) => value switch
    {
        "open" => InquiryStatus.Open,
        "in_progress" => InquiryStatus.InProgress,
        "resolved" => InquiryStatus.Resolved,
        "closed" => InquiryStatus.Closed,
        _ => InquiryStatus.Open
    };
}