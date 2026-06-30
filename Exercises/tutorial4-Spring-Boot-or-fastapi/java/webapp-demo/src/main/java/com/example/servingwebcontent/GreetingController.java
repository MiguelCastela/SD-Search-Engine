package com.example.servingwebcontent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import com.example.servingwebcontent.beans.Number;
import com.example.servingwebcontent.forms.Project;
import com.example.servingwebcontent.thedata.Employee;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Controller
public class GreetingController {

    private final List<Project> projectList = new CopyOnWriteArrayList<>();
    private static final Map<String, String> users = new HashMap<>();
    private static final Map<String, List<Project>> userProjects = new HashMap<>();

    static {
        users.put("admin", "password"); // username: admin, password: password
        users.put("user", "1234");     // username: user, password: 1234
    }

    @Resource(name = "requestScopedNumberGenerator")
    private Number nRequest;

    @Resource(name = "sessionScopedNumberGenerator")
    private Number nSession;

    @Resource(name = "applicationScopedNumberGenerator")
    private Number nApplication;

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public Number requestScopedNumberGenerator() {
        return new Number();
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public Number sessionScopedNumberGenerator() {
        return new Number();
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_APPLICATION, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public Number applicationScopedNumberGenerator() {
        return new Number();
    }

    @GetMapping("/")
    public String redirect() {
        return "redirect:/greeting";
    }

	@GetMapping("/greeting")
	public String greeting(@RequestParam(name="name", required=false, defaultValue="World") String name, Model model) {
		model.addAttribute("name", name);
		model.addAttribute("othername", "SD");
		return "greeting";
	}

    @GetMapping("/givemeatable")
	public String atable(Model model) {
        Employee [] theEmployees = { new Employee(1, "José", "9199999", 1890), new Employee(2, "Marisa", "9488444", 2120), new Employee(3, "Hélio", "93434444", 2500)};
        List<Employee> le = new ArrayList<>();
        Collections.addAll(le, theEmployees);
        model.addAttribute("emp", le);
		return "table";
	}

    // from https://attacomsian.com/blog/spring-boot-thymeleaf-form-handling and https://github.com/attacomsian/code-examples
	@GetMapping("/create-project")
    public String createProjectForm(Model model) {
        
        model.addAttribute("project", new Project());
        return "create-project";
    }

    @PostMapping("/save-project")
    public String saveProjectSubmission(@ModelAttribute Project project, HttpSession session, Model model) {
        String username = (String) session.getAttribute("user");
        if (username == null) {
            return "redirect:/login"; // Redirect to login if the user is not logged in
        }

        // Get or create the user's project list
        userProjects.putIfAbsent(username, new ArrayList<>());
        userProjects.get(username).add(project);

        model.addAttribute("project", project);
        return "result";
    }

    @GetMapping("/projects")
    public String listProjects(HttpSession session, Model model) {
        String username = (String) session.getAttribute("user");
        if (username == null) {
            return "redirect:/login"; // Redirect to login if the user is not logged in
        }

        // Get the user's projects
        List<Project> projects = userProjects.getOrDefault(username, new ArrayList<>());
        model.addAttribute("projects", projects);
        return "projects";
    }

    @GetMapping("/counters")
	public String counters(Model model) {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = attr.getRequest().getSession(true);
        Object counter = session.getAttribute("counter");
        int c;
        if (counter == null){
            c = 1;
        }else if (counter instanceof Integer) {
            c = (Integer) counter + 1;
        } else {
            // Handle unexpected types (optional)
            c = 1;
        }
        session.setAttribute("stringValue", "Hello, Session!");
        String stringValue = (String) session.getAttribute("stringValue");
        session.setAttribute("counter", c);
        model.addAttribute("stringValue", stringValue);
		model.addAttribute("sessioncounter", c);
		model.addAttribute("requestcounter2", this.nRequest.next());
		model.addAttribute("sessioncounter2", this.nSession.next());
		model.addAttribute("applicationcounter2", this.nApplication.next());
		return "counter";
	}

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password.");
        }
        return "login"; // This corresponds to your login.html template
    }

    @PostMapping("/login")
    public String login(HttpSession session, @RequestParam String username, @RequestParam String password) {
        if (users.containsKey(username) && users.get(username).equals(password)) {
            session.setAttribute("user", username);
            return "redirect:/greeting";
        }
        return "redirect:/login?error=true";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

}