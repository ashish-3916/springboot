package com.start.course;

import com.start.topic.Topic;
import com.start.topic.TopicController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class CourseController {


    private List<TopicController> topicController;

    @Autowired
    private List<TopicController> topicController1;

    private CourseService courseService;
    int i;


    @Autowired
    private CourseService courseService2;

    @Autowired
    CourseController(CourseService courseService , List<TopicController> topicController ){

        this.courseService = courseService;
        this.topicController = topicController;
    }

    @RequestMapping("topics/{topicId}/courses")
    public List<Course> getAllCourses(@PathVariable String topicId){
        return courseService.getAllCourses(topicId); // to do
    }

    @RequestMapping("topics/{topicId}/courses/{Courseid}")
    public Optional<Course> getCourse(@PathVariable String Courseid){
        return courseService.getCourse(Courseid);
    }

    @RequestMapping(method = RequestMethod.POST , value = "topics/{topicId}/courses")
    public void addCourse(@RequestBody Course course , @PathVariable String topicId){ // here HTTP would send the json format of the body , but will be automaticallly converted into Course Format
        course.setTopic(new Topic(topicId, "" , ""));
        courseService.addCourse(course);
    }

    @RequestMapping(method = RequestMethod.PUT , value = "topics/{topicId}/courses/{courseId}")
    public void updateCourse(@RequestBody Course course , @PathVariable String courseId, @PathVariable String topicId){
        course.setTopic(new Topic(topicId, "" , ""));
        courseService.updateCourse(course);
    }

    @RequestMapping(method = RequestMethod.DELETE , value = "topics/{topicId}/courses/{id}")
    public void deleteCourse(@PathVariable String id){
        courseService.deleteCourse(id);
    }
}
