package com.start.topic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TopicService {


    private TopicRepository topicRepository ;


    @Autowired
    TopicService(TopicRepository a){
        this.topicRepository =a ;
    }

    public List<Topic> getAllTopics(){
        List<Topic> topics =  new ArrayList<>();
        topicRepository.findAll().forEach(topics::add);
        return  topics;
    }
    public Optional<Topic> getTopic(String id){
        return topicRepository.findById(id);
    }

    public void add_updateTopic(Topic topic) {
        topicRepository.save(topic);
    }

    public void deleteTopic(String id) {
        topicRepository.deleteById(id);
    }
}
