package xyz.fz.fire.fight.configuration;

import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.text.PropertiesRealm;
import org.apache.shiro.spring.web.config.DefaultShiroFilterChainDefinition;
import org.apache.shiro.spring.web.config.ShiroFilterChainDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShiroConfiguration {

    @Bean
    public Realm realm() {
        return new PropertiesRealm();
    }

    @Bean
    public ShiroFilterChainDefinition shiroFilterChainDefinition() {
        DefaultShiroFilterChainDefinition chainDefinition = new DefaultShiroFilterChainDefinition();
        chainDefinition.addPathDefinition("/login.html", "anon");
        chainDefinition.addPathDefinition("/doLogin", "anon");
        chainDefinition.addPathDefinition("/task.html", "authc");
        chainDefinition.addPathDefinition("/doLogout", "authc");
        chainDefinition.addPathDefinition("/list", "authc, perms[task:ls]");
        chainDefinition.addPathDefinition("/execute", "authc, perms[task:exec]");
        chainDefinition.addPathDefinition("/console", "authc, perms[task:read]");
        chainDefinition.addPathDefinition("/**", "authc, perms[no]");
        return chainDefinition;
    }

}
