package com.lk.microservicio_seguridad.Controllers;

import com.lk.microservicio_seguridad.Repositories.UserRoleRepository;
import com.lk.microservicio_seguridad.Services.UserRoleService;
import com.lk.microservicio_seguridad.models.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/user-role")
public class UserRoleController {
    @Autowired
    private UserRoleService theUserRoleService;
    @Autowired
    private UserRoleRepository userRoleRepository;

    @PostMapping("user/{userId}/role/{roleId}")
    public ResponseEntity<Map<String, String>> addUserRole(
            @PathVariable String userId,
            @PathVariable String roleId) {

        boolean response = this.theUserRoleService.addUserRole(userId, roleId);
        if (response) {
            return ResponseEntity.ok(Map.of("message", "Success"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User or Role not found"));
        }
    }
    @DeleteMapping("{userRoleId}")
    public ResponseEntity<Map<String, String>> removeUserRole(
            @PathVariable String userRoleId) {

        boolean response = this.theUserRoleService.removeUserRole(userRoleId);
        if (response) {
            return ResponseEntity.ok(Map.of("message", "Success"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User or Role not found"));
        }
    }

    @GetMapping("/user/{userId}")
    public List<UserRole> getRolesByUser(@PathVariable String userId) {
        return userRoleRepository.findByUserId(userId);
    }
}
