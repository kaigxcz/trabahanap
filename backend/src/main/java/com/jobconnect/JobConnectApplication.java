package com.jobconnect;

import com.jobconnect.model.Job;
import com.jobconnect.model.User;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class JobConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobConnectApplication.class, args);
    }

    @Bean
    CommandLineRunner seedData(JobRepository jobRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Seed admin if not exists
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User("admin", passwordEncoder.encode("admin123"), "Admin", "User", "admin@jobconnect.com", "Manila");
                admin.setRole("ADMIN");
                userRepository.save(admin);
            }
            // Seed employer if not exists
            if (!userRepository.existsByUsername("employer1")) {
                User employer = new User("employer1", passwordEncoder.encode("employer123"), "Tech", "Recruiter", "hr@techstart.com", "Quezon City");
                employer.setRole("EMPLOYER");
                userRepository.save(employer);
            }
            // Seed jobs only if empty
            if (jobRepository.count() == 0) {
                jobRepository.save(new Job("Frontend Engineer", "Shopify", "Remote", "₱80k-₱120k", "Technology", "💎", "Full-time", "1-3 years", "Build and maintain high-quality web interfaces for our e-commerce platform.", "React, TypeScript, CSS, REST APIs, Git"));
                jobRepository.save(new Job("UI/UX Designer", "Creative Studio", "Manila", "₱45k-₱60k", "Creative", "🎨", "Contract", "Fresh/Entry", "Create beautiful and intuitive designs for web and mobile applications.", "Figma, Adobe XD, Prototyping, User Research"));
                jobRepository.save(new Job("Accountant", "FinancePro", "Makati", "₱40k-₱55k", "Finance", "🏦", "Full-time", "2-5 years", "Manage financial records, prepare reports, and ensure compliance with local tax regulations.", "CPA License, QuickBooks, Excel, BIR Filing"));
                jobRepository.save(new Job("Full Stack Developer", "TechStart", "Quezon City", "₱90k-₱130k", "Technology", "💻", "Full-time", "3-5 years", "Design and build scalable web applications from frontend to backend.", "Java, Spring Boot, React, PostgreSQL, Docker"));
                jobRepository.save(new Job("Data Analyst", "DataCorp", "Remote", "₱60k-₱90k", "Technology", "📊", "Full-time", "1-3 years", "Analyze large datasets to uncover business insights and support data-driven decisions.", "Python, SQL, Tableau, Excel, Statistics"));
                jobRepository.save(new Job("Graphic Designer", "PixelWorks", "Cebu", "₱35k-₱50k", "Creative", "✏️", "Part-time", "Fresh/Entry", "Create compelling visual content for social media, print, and digital campaigns.", "Photoshop, Illustrator, Canva, Branding"));
                jobRepository.save(new Job("Registered Nurse", "MedCare Hospital", "Pasig", "₱30k-₱45k", "Healthcare", "🏥", "Full-time", "Fresh/Entry", "Provide quality patient care in a busy hospital setting.", "PRC License, BLS/ACLS, Patient Care, EMR"));
                jobRepository.save(new Job("High School Teacher", "Bright Minds Academy", "Davao", "₱25k-₱35k", "Education", "📚", "Full-time", "1-3 years", "Deliver engaging lessons in Mathematics and Science for high school students.", "LET License, Lesson Planning, Classroom Management"));
                jobRepository.save(new Job("Digital Marketing Specialist", "GrowthLab", "Remote", "₱40k-₱65k", "Marketing", "📣", "Full-time", "1-3 years", "Plan and execute digital marketing campaigns across SEO, SEM, and social media.", "Google Ads, Meta Ads, SEO, Analytics, Copywriting"));
                jobRepository.save(new Job("Backend Developer", "CloudSys", "Taguig", "₱70k-₱100k", "Technology", "⚙️", "Full-time", "3-5 years", "Build robust and scalable APIs and microservices.", "Java, Spring Boot, MySQL, Redis, AWS"));
                jobRepository.save(new Job("Mobile Developer", "AppForge", "Remote", "₱75k-₱110k", "Technology", "📱", "Full-time", "1-3 years", "Build cross-platform mobile apps for iOS and Android.", "Flutter, Dart, Firebase, REST APIs"));
                jobRepository.save(new Job("DevOps Engineer", "InfraCloud", "Taguig", "₱100k-₱150k", "Technology", "🔧", "Full-time", "3-5 years", "Manage CI/CD pipelines, cloud infrastructure, and system reliability.", "AWS, Docker, Kubernetes, Terraform, Linux"));
                jobRepository.save(new Job("Content Writer", "MediaHub", "Remote", "₱25k-₱40k", "Creative", "✍️", "Part-time", "Fresh/Entry", "Write engaging blog posts, articles, and social media content for various clients.", "SEO Writing, WordPress, Research, Copywriting"));
                jobRepository.save(new Job("Financial Analyst", "InvestCo", "Makati", "₱55k-₱80k", "Finance", "📈", "Full-time", "1-3 years", "Analyze financial data and prepare investment reports for senior management.", "Excel, Financial Modeling, Bloomberg, CFA"));
                jobRepository.save(new Job("Physical Therapist", "RehabPlus", "Quezon City", "₱35k-₱50k", "Healthcare", "💪", "Full-time", "Fresh/Entry", "Provide rehabilitation services to patients recovering from injuries and surgeries.", "PRC License, Patient Assessment, Rehabilitation"));
                jobRepository.save(new Job("College Instructor", "State University", "Manila", "₱30k-₱45k", "Education", "🎓", "Full-time", "1-3 years", "Teach undergraduate courses in Computer Science and Information Technology.", "LET License, Programming, Curriculum Design"));
                jobRepository.save(new Job("Social Media Manager", "BrandBoost", "Remote", "₱35k-₱55k", "Marketing", "📲", "Full-time", "1-3 years", "Manage social media accounts, create content calendars, and grow brand presence.", "Instagram, TikTok, Facebook Ads, Canva, Analytics"));
                jobRepository.save(new Job("Cybersecurity Analyst", "SecureNet", "Taguig", "₱90k-₱130k", "Technology", "🔒", "Full-time", "3-5 years", "Monitor and protect company systems from cyber threats and vulnerabilities.", "SIEM, Penetration Testing, ISO 27001, Networking"));
                jobRepository.save(new Job("Video Editor", "ContentFactory", "Cebu", "₱30k-₱45k", "Creative", "🎬", "Contract", "Fresh/Entry", "Edit video content for YouTube, social media, and corporate presentations.", "Premiere Pro, After Effects, DaVinci Resolve"));
                jobRepository.save(new Job("HR Specialist", "PeopleFirst", "Mandaluyong", "₱35k-₱50k", "Finance", "👥", "Full-time", "1-3 years", "Handle recruitment, onboarding, and employee relations for a growing company.", "HRIS, Recruitment, Labor Law, Communication"));
            }
        };
    }
}
