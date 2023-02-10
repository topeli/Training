package org.example.TonyaController;

import org.example.models.Rectangle;
import org.example.services.RectangleService;
import org.example.services.StudentService;
@ComponentScan("com.example.demo.controllers")


@RestController
@RequestMapping("/telegram")
public class MainController
{
    private final RectangleService rectangleService;
    public MainController(RectangleService rectangleService) {
        this.rectangleService = rectangleService;
    }
    @PostMapping("/Rectangle")
    public int Rectangle(@RequestBody Rectangle rec) {return rectangleService.Rectangle(rec);}
}
