package com.devproject.bot;

import org.dreambot.api.input.Mouse;
import org.dreambot.api.input.event.impl.mouse.MouseButton;
import org.dreambot.api.input.mouse.algorithm.MouseAlgorithm;
import org.dreambot.api.input.mouse.destination.AbstractMouseDestination;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.input.mouse.MouseSettings;
import org.dreambot.api.utilities.Logger;

import java.awt.*;
import java.util.List;

public class WindMouseML implements MouseAlgorithm {
    private int mouseSpeed = (int) (Math.max(30, MouseSettings.getSpeed()) / 1.3);
    private final int mouseSpeedLow = Math.round(mouseSpeed / 4);
    private int mouseGravity = Calculations.random(15, 30);
    private int mouseWind = Calculations.random(1, 3);

    private final List<Point> movementData;

    public WindMouseML(List<Point> movementData) {
        this.movementData = movementData;
    }

    @Override
    public boolean handleMovement(AbstractMouseDestination abstractMouseDestination) {
        Point target = abstractMouseDestination.getSuitablePoint();
        executeMouseMovement(target);
        return distance(Mouse.getPosition(), target) < 2;
    }

    @Override
    public boolean handleClick(MouseButton mouseButton) {
        return Mouse.getDefaultMouseAlgorithm().handleClick(mouseButton);
    }

    private void executeMouseMovement(Point target) {
        Point current = Mouse.getPosition();
        for (Point step : movementData) {
            Point adjustedStep = adjustToTarget(step, target, current);
            setMousePosition(adjustedStep);
            sleep(Calculations.random(5, 25));
        }
    }

    private Point adjustToTarget(Point step, Point target, Point current) {
        int adjustedX = current.x + (int) ((step.x / 100.0) * (target.x - current.x));
        int adjustedY = current.y + (int) ((step.y / 100.0) * (target.y - current.y));
        return new Point(adjustedX, adjustedY);
    }

    private void setMousePosition(Point point) {
        Mouse.hop(point);
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Logger.log(e.getMessage());
        }
    }

    private double distance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }
}
