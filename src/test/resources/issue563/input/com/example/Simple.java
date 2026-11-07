package com.example;

import org.example.AbstractAutowireCapableBeanFactory;
import org.example.ConfigurableListableBeanFactory;
import org.example.DefaultListableBeanFactory;

class Simple {
  private boolean allowCircularReferences;
  private boolean allowBeanDefinitionOverriding;

  void bar(ConfigurableListableBeanFactory beanFactory) {
    if (beanFactory instanceof AbstractAutowireCapableBeanFactory autowireCapableBeanFactory) {
      autowireCapableBeanFactory.setAllowCircularReferences(this.allowCircularReferences);
      if (beanFactory instanceof DefaultListableBeanFactory listableBeanFactory) {
        listableBeanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
      }
    }
  }
}
