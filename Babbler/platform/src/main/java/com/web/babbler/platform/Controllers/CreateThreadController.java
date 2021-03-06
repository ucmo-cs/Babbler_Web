package com.web.babbler.platform.Controllers;

import com.web.babbler.platform.models.Threads;
import com.web.babbler.platform.services.StorageService;
import com.web.babbler.platform.services.ThreadService;
import com.web.babbler.platform.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@Controller
public class CreateThreadController
{
    List<String> fileList = new ArrayList<>();
    StorageService storageService;
    ThreadService threadService;
    Threads thread;
    String currentUserName;
    UserService userService;
    @GetMapping("/create")
    public String loadCreateThreadPage(Model model) {
        Authentication user_authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = user_authentication.getName();
        if (!currentUserName.equals("") && !currentUserName.equals("anonymousUser")) {
            model.addAttribute("currentUser", currentUserName);
        } else {
            return "error";
        }
        fileList = new ArrayList<>();
        model.addAttribute("createdThread",new Threads());
        System.out.println("Switching over to the create thread page.");
        return "createThread";
    }
    @PostMapping("/create")
    public String addToThread(@ModelAttribute("files") MultipartFile[] files, Model model) throws Exception {
        Authentication user_authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = user_authentication.getName();
        if (!currentUserName.equals("") && !currentUserName.equals("anonymousUser")) {
            model.addAttribute("currentUser", currentUserName);
        }
        storageService = new StorageService();
        for (MultipartFile file : files) {
            if(fileList.size() > 4)
            {
                model.addAttribute("Photos", fileList);
                model.addAttribute("createdThread", new Threads());
                model.addAttribute("capacityMessage","You can only upload five photos per post.");
                return "createThread";
            }
            storageService.uploadFile(file);
            fileList.add(storageService.getUploadURL(file.getOriginalFilename()));
        }
        model.addAttribute("createdThread",new Threads());
        model.addAttribute("Photos", fileList);
        return "createThread";
    }
    @PostMapping("/create/save")
    public String saveCreatedThread(@ModelAttribute("createdThread") Threads capturedThread, Model model) throws ExecutionException, InterruptedException, IOException {
        storageService = new StorageService();
        threadService = new ThreadService();
        thread = new Threads();
        thread.setTitle(capturedThread.getTitle());
        thread.setDate(getCurrentDate());
        thread.setSender(getUserIDForThread());
        thread.setCaption(capturedThread.getCaption());
        thread.setTrueCrime(capturedThread.isTrueCrime());
        System.out.println("Answer " + capturedThread.isTrueCrime());
        thread.setScore(0);
        for(int i = 0; i < fileList.size(); i++)
        {
            if(i == 0) {
                thread.setImage1(fileList.get(i));
            } else if (i == 1) {
                thread.setImage2(fileList.get(i));
            } else if (i == 2) {
                thread.setImage3(fileList.get(i));
            } else if (i == 3) {
                thread.setImage4(fileList.get(i));
            } else if (i == 4) {
                thread.setImage5(fileList.get(i));
            }
        }
        if(threadService.uniqueThread(thread.getTitle(),thread.getSender()))
        {
            threadService.createThread(thread);
            threadService.addThreadToUser(thread);
        } else {
            model.addAttribute("reoccurringTitle", "You have already used this title in a previous thread");
            return "createThread";
        }
        return "redirect:/";
    }
    public String getUserIDForThread() throws ExecutionException, InterruptedException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        currentUserName = authentication.getName();
        userService = new UserService();
        return userService.getUserID(currentUserName);
    }
    public String getCurrentDate() {
        return new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(new Date());
    }
}
