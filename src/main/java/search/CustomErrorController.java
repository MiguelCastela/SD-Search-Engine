// filepath: [CustomErrorController.java](http://_vscodecontentref_/7)
package search;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.boot.web.servlet.error.ErrorController;
/**
 * @authors
 * Miguel Castela 2022212972 👍
 * Miguel Martins 2022213951 👍
 */

/**
 * Custom error controller for handling errors in the application.
 * This class implements the ErrorController interface and provides a method
 * to handle errors and display an error page.
 */
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        model.addAttribute("message", message != null ? message : "Something went wrong");
        model.addAttribute("statusCode", statusCode);

        return "error"; // renders error.html
    }
}