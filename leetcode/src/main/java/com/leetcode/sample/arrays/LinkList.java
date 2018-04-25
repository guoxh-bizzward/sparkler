package com.leetcode.sample.arrays;

public class LinkList<T> {
    private Node head;
    private Node tail;


    class Node{
        private Node next;
        private T data;

        public Node(){

        }
        public Node(T data,Node next){
            this.data = data;
            this.next = next;
        }
    }
}
