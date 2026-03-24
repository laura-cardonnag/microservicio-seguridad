package com.lk.microservicio_seguridad.Controllers;

import com.lk.microservicio_seguridad.Repositories.RoleRepository;
import com.lk.microservicio_seguridad.models.Role;
import com.lk.microservicio_seguridad.Services.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/private/roles")
public class RoleController {

    @Autowired
    private RoleService theRoleService;
    private RoleRepository theRoleRepository;

    @GetMapping("")
    public List<Role> find() {
        return this.theRoleService.find();
    }

    @GetMapping("{id}")
    public Role findById(@PathVariable String id) {
        return this.theRoleService.findById(id);
    }

    @PostMapping
    public Role create(@RequestBody Role newRole) {
        return this.theRoleService.create(newRole);
    }

    @PutMapping("{id}")
    public Role update(@PathVariable String id, @RequestBody Role newRole) {
        return this.theRoleService.update(id, newRole);
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable String id) {
        this.theRoleService.delete(id);
    }
}