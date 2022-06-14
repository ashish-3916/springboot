package com.start.course;

import com.start.topic.TopicRepository;
import com.start.topic.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository ;

    private TopicRepository topicRepository;

    @Autowired
    CourseService(TopicRepository topicRepository){
        this.topicRepository = topicRepository;
    }

    public List<Course> getAllCourses(String id){
        List<Course> courses =  new ArrayList<>();
        courseRepository.findByTopicId(id).forEach(courses::add); // to do
        return  courses;
    }
    public Optional<Course> getCourse(String id){
        return courseRepository.findById(id);
    }

    public void addCourse(Course course) {
        courseRepository.save(course);
    }
    public void updateCourse(Course course) {
        courseRepository.save(course);
    }

    public void deleteCourse(String id) {
        courseRepository.deleteById(id);
    }
}
