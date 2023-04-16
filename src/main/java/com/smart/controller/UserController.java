package com.smart.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;
	
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		User user = userRepository.getUserByUsername(userName);
		System.out.println(user);
		model.addAttribute("user", user);

	}

	@RequestMapping("/index")
	public String dashboard(Model model) {
		model.addAttribute("title", "User Dashboard");

		return "normal/user_dashboard";
	}

	// open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {

		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());

		return "normal/add_contact_form";
	}

	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute("contact") Contact contact,
			@RequestParam("profileImg") MultipartFile file, Principal principal, Model model, HttpSession session) {

		

		// processing and uploading file..
		try {
			
			String name = principal.getName();
			User user = this.userRepository.getUserByUsername(name);
			
			if (file.isEmpty()) {
				System.out.println("file is empty");
			} else {

				String fileName = StringUtils.cleanPath(file.getOriginalFilename());
				contact.setImage(fileName);
				File uploadDir = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(uploadDir.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

			}
			
			System.out.println("image is uploaded");

			contact.setUser(user);
			user.getContacts().add(contact);


			// save user
			this.userRepository.save(user);

			System.out.println("data" + contact);
			System.out.println("Added to database");
			
			session.setAttribute("message", new Message("Your contact is added !! Add more ...", "success"));


		} catch (Exception e) {
			System.out.println("ERROR" + e.getMessage());
			e.printStackTrace();
			session.setAttribute("message", new Message("Something went wrong !! Try again ...", "danger"));
		}
		
		return "redirect:/user/add-contact";
	}

	
	//per page 5[n]
	//current page 0[page]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(
			@PathVariable("page") int page
			,Model model, Principal principal)
	{
		String name = principal.getName();
		User user = this.userRepository.getUserByUsername(name);
		
		Pageable pageable =PageRequest.of(page, 5);
		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);
		
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", contacts.getTotalPages());
		
		return "normal/show_contacts";
	}
	
	@GetMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		//
		String username = principal.getName();
		User user = this.userRepository.getUserByUsername(username);
		
		if(user.getId()==contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}
		
		return "normal/contact_detail";
	}
	
	//delete contact handler\
	@GetMapping("/delete/{cId}")
	@Transactional
	public String deleteContact(@PathVariable("cId") Integer cId, Model model, HttpSession session,Principal principal) {
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		User user = this.userRepository.getUserByUsername(principal.getName());
		
		user.getContacts().remove(contact);
		this.userRepository.save(user);
		
		session.setAttribute("message", new Message("Contact deleted successfully", "success"));
		
		return "redirect:/user/show-contacts/0";
	}
	
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer  cid,Model model) {
		
		model.addAttribute("title", "Update Contact");
		Contact contact = this.contactRepository.findById(cid).get();
		
		model.addAttribute("contact", contact);
		
		
		return "normal/update_form";
	}
	
	@PostMapping("/process-update")
	public String processUpdate(@ModelAttribute("contact") Contact contact, 
			@RequestParam("profileImg") MultipartFile file, 
			Model model, 
			HttpSession session,
			Principal principal) {
		
		
		
		try {
			
			Contact oldContact = this.contactRepository.findById(contact.getcId()).get();
			
			
			if(!file.isEmpty()) {
				
				
				//delete old photo
				
				File deleteFile=  new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile, oldContact.getImage());
				file1.delete();
				
				
				File uploadDir = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(uploadDir.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
				
				
			}else {
				contact.setImage(oldContact.getImage());
			}
			
			
			User user = this.userRepository.getUserByUsername(principal.getName());
			System.out.println("CID CONTACT "+contact.getcId());
			contact.setUser(user);
			this.contactRepository.save(contact);
	
			session.setAttribute("message", new Message("Your Contact is Updated..","success"));
			System.out.println("Contact "+contact);
			
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
	//your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		
		model.addAttribute("title", "Profile Page");
		
		
		return "normal/profile";
	}
	
	
	
	
	
}
