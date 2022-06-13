package com.start.course;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends CrudRepository<Course, String> {
    public List<Course> findByTopicId(String topicId);
}
