import { useState, useEffect } from "react";
import { Login } from "./components/Login";
import { GalaxyView } from "./components/GalaxyView";
import { TaskDashboard } from "./components/TaskDashboard";
import { AIChat } from "./components/AIChat";
import { Shop } from "./components/Shop";
import { CheckCircle, AlertCircle } from "lucide-react";
import { Statistics } from "./components/Statistics";
import { Profile } from "./components/Profile";
import { NavigationBar } from "./components/NavigationBar";
import { AddTaskModal } from "./components/AddTaskModal";
import { AddHabitModal } from "./components/AddHabitModal";
import { EditHabitModal } from "./components/EditHabitModal";
import { HabitTracker } from "./components/HabitTracker";
import confetti from 'canvas-confetti';
import { UserProvider, useUser } from "../context/UserContext";
import { tasksApi, TaskResponse, AddTaskRequest } from "../services/taskApi";
import { habitsApi, HabitResponse, AddHabitRequest, UpdateHabitRequest } from "../services/habitsApi";
import { storeApi, ItemCatalogResponse } from "../services/storeApi";
import { aiApi } from "../services/aiApi";

type Screen = 'tasks' | 'habits' | 'galaxy' | 'chat' | 'profile' | 'shop' | 'statistics';

interface PlanetData {
  id: string;
  name: string;
  type: string;
  size: number;
  position: { x: number; y: number };
  imageUrl?: string;
}

interface ChatMessage {
  id: string;
  type: 'ai' | 'user' | 'task-suggestion';
  content: string;
  timestamp: Date;
  taskData?: any;
}

interface ShopItemState {
  id: string;
  name: string;
  type: any;
  price: number;
  description: string;
  unlocked: boolean;
  imageUrl?: string;
  positionX?: number;
  positionY?: number;
  scale?: number;
}

function MainApp() {
  const { user, spendCoins } = useUser();
  const [localCoins, setLocalCoins] = useState(user?.totalCoins || 0);
  const coinBalance = localCoins;
  const [currentScreen, setCurrentScreen] = useState<Screen>('galaxy');
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const triggerConfetti = () => {
    confetti({
      particleCount: 100,
      spread: 90,
      origin: { y: 0.6 },
      scalar: 2,
      gravity: 1.2,
      ticks: 150,
      colors: ['#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#ec4899'],
    });
  };

  const [tasks, setTasks] = useState<TaskResponse[]>([]);
  const [habits, setHabits] = useState<HabitResponse[]>([]);
  const [shopItems, setShopItems] = useState<ShopItemState[]>([]);
  const [planets, setPlanets] = useState<PlanetData[]>([]);

  useEffect(() => {
    const loadData = async () => {
      try {
        const [fetchedTasks, fetchedHabits] = await Promise.all([
          tasksApi.getTasks(),
          habitsApi.getHabits()
        ]);
        setTasks(fetchedTasks);
        setHabits(fetchedHabits);
      } catch (error) {
        console.error(error);
      }
    };

    const loadStoreData = async () => {
      try {
        const [catalogItems, myItems] = await Promise.all([
          storeApi.getAvailableItems(),
          storeApi.getMyItems()
        ]);

        const ownedCatalogIds = new Set(myItems.map((item: any) => item.catalogItemId));

        const formattedShopItems: ShopItemState[] = catalogItems.map((item: any) => ({
          id: item.id.toString(),
          name: item.itemName,
          type: item.itemType || item.type || 'earth',
          price: item.costCoins,
          description: item.description || 'A celestial object',
          unlocked: ownedCatalogIds.has(item.id),
          imageUrl: item.imageUrl,
          positionX: item.positionX,
          positionY: item.positionY,
          scale: item.scale
        }));

        setShopItems(formattedShopItems);

        const typeCount: Record<string, number> = {};
        const myPlanetsData = myItems.map((myVal: any) => {
          const catalogInfo = catalogItems.find((c: any) => c.id === myVal.catalogItemId);
          const itemType = (catalogInfo?.itemType || catalogInfo?.type || 'planet').toLowerCase();
          typeCount[itemType] = (typeCount[itemType] || 0) + 1;
          const duplicateIndex = typeCount[itemType] - 1;

          let baseSize = 130;
          let baseX = 50;
          let baseY = 50;

          if (itemType === 'star' || itemType === 'sun') {
            baseSize = 150;
            baseX = 10;
            baseY = 50;
          } else {
            baseSize = 200;
            baseX = 30 + (duplicateIndex * 15);
            baseY = 50 + (duplicateIndex % 2 === 0 ? 8 : -8);
          }

          if (catalogInfo?.scale && Number(catalogInfo.scale) > 0) baseSize = Number(catalogInfo.scale);
          if (catalogInfo?.positionX) baseX = Number(catalogInfo.positionX);
          if (catalogInfo?.positionY) baseY = Number(catalogInfo.positionY);

          return {
            id: myVal.id.toString(),
            name: catalogInfo?.itemName || 'Unknown Planet',
            type: itemType,
            size: baseSize,
            position: { x: baseX, y: baseY },
            imageUrl: catalogInfo?.imageUrl
          };
        });

        setPlanets(myPlanetsData);
      } catch (error) {
        console.error(error);
      }
    };

    loadData();
    if (user) loadStoreData();
  }, [user]);

  const handleTaskToggle = async (taskId: number) => {
    const task = tasks.find(t => t.taskId === taskId);
    if (!task || task.status === 'COMPLETED') return;
    try {
      const response = await tasksApi.completeTask(taskId);
      setTasks(tasks.map(t => t.taskId === taskId ? { ...t, status: 'COMPLETED' } : t));
      setLocalCoins(response.newTotalCoins);
      triggerConfetti();
      showToast(`Mission Accomplished! +${response.coinsEarned} coins`);
    } catch (error) {
      showToast("Error", "error");
    }
  };

  const handleTaskDelete = async (taskId: number) => {
    try {
      await tasksApi.deleteTask(taskId);
      setTasks(tasks.filter(task => task.taskId !== taskId));
    } catch (error) {
      console.error(error);
    }
  };

  const handleCreateNewTask = async (taskData: AddTaskRequest) => {
    try {
      const newTask = await tasksApi.createTask(taskData);
      setTasks([newTask, ...tasks]);
      setIsAddTaskModalOpen(false);
    } catch (error) {
      console.error(error);
    }
  };

  const [isEditHabitModalOpen, setIsEditHabitModalOpen] = useState(false);
  const [habitToEdit, setHabitToEdit] = useState<HabitResponse | null>(null);

  const handleHabitCheck = async (habitId: number) => {
    try {
      const response = await habitsApi.completeHabit(habitId);
      setLocalCoins(response.newTotalCoins);
      setHabits(habits.map(habit => {
        if (habit.habitId === habitId) {
          const today = new Date().toISOString().split('T')[0];
          return { ...habit, currentStreak: response.currentStreak, bestStreak: response.bestStreak, lastCompletedDate: today, totalCompletions: habit.totalCompletions + 1 };
        }
        return habit;
      }));
      triggerConfetti();
      showToast(`Habit Logged! +${response.coinsEarned} coins`);
    } catch (error) {
      showToast("Error", "error");
    }
  };

  const handleAddHabit = async (habitData: AddHabitRequest) => {
    try {
      const newHabit = await habitsApi.createHabit(habitData);
      setHabits([newHabit, ...habits]);
      setIsAddHabitModalOpen(false);
    } catch (error) {
      console.error(error);
    }
  };

  const handleUpdateHabit = async (habitId: number, data: UpdateHabitRequest) => {
    try {
      const updatedHabit = await habitsApi.updateHabit(habitId, data);
      setHabits(habits.map(habit => habit.habitId === habitId ? { ...habit, ...updatedHabit } : habit));
      setIsEditHabitModalOpen(false);
    } catch (error) {
      console.error(error);
    }
  };

  const handleDeleteHabit = async (habitId: number) => {
    try {
      await habitsApi.deleteHabit(habitId);
      setHabits(habits.filter(habit => habit.habitId !== habitId));
    } catch (error) {
      console.error(error);
    }
  };

  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [isAiLoading, setIsAiLoading] = useState(false);
  const [chatHistoryLoaded, setChatHistoryLoaded] = useState(false);

  const [isAddTaskModalOpen, setIsAddTaskModalOpen] = useState(false);
  const [isAddHabitModalOpen, setIsAddHabitModalOpen] = useState(false);

  // Load chat history the first time the user opens the chat screen
  useEffect(() => {
    if (currentScreen === 'chat' && !chatHistoryLoaded && user) {
      aiApi.getHistory().then((history) => {
        const historyMessages: ChatMessage[] = history
          .slice()
          .reverse()
          .flatMap((turn) => [
            { id: `u-${turn.conversationId}`, type: 'user' as const, content: turn.userMessage, timestamp: new Date(turn.createdAt) },
            { id: `a-${turn.conversationId}`, type: 'ai' as const, content: turn.aiResponse, timestamp: new Date(turn.createdAt) },
          ]);
        if (historyMessages.length > 0) {
          setChatMessages(historyMessages);
        }
        setChatHistoryLoaded(true);
      }).catch(() => setChatHistoryLoaded(true));
    }
  }, [currentScreen, chatHistoryLoaded, user]);

  const appendAiMessage = (content: string) => {
    setChatMessages(prev => [...prev, {
      id: Date.now().toString(),
      type: 'ai',
      content,
      timestamp: new Date(),
    }]);
  };

  const handleSendMessage = async (message: string, newConversation?: boolean) => {
    if (newConversation) {
      setChatMessages([]);
      return;
    }
    if (!message.trim()) return;

    setChatMessages(prev => [...prev, {
      id: Date.now().toString(),
      type: 'user',
      content: message,
      timestamp: new Date(),
    }]);
    setIsAiLoading(true);

    try {
      const response = await aiApi.sendMessage({
        message,
        newConversation: false,
        userTimezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      });
      appendAiMessage(response.aiMessage);
      if (response.tasksCreated > 0) {
        const updatedTasks = await tasksApi.getTasks();
        setTasks(updatedTasks);
        showToast(`AI created ${response.tasksCreated} task${response.tasksCreated > 1 ? 's' : ''} for you!`);
      }
      if (response.habitsCreated > 0) {
        const updatedHabits = await habitsApi.getHabits();
        setHabits(updatedHabits);
        showToast(`AI created ${response.habitsCreated} habit${response.habitsCreated > 1 ? 's' : ''} for you!`);
      }
    } catch {
      appendAiMessage("Sorry, I'm having trouble connecting right now. Please try again.");
    } finally {
      setIsAiLoading(false);
    }
  };

  const handleDailyBriefing = async () => {
    setIsAiLoading(true);
    try {
      const response = await aiApi.getDailyBriefing();
      appendAiMessage(response.aiMessage);
    } catch {
      appendAiMessage("Couldn't load your daily briefing. Please try again.");
    } finally {
      setIsAiLoading(false);
    }
  };

  const handlePurchase = async (itemId: string) => {
    const item = shopItems.find(i => i.id === itemId);
    if (!item || item.unlocked || coinBalance < item.price) return;

    try {
      const response = await storeApi.buyItem(Number(itemId));
      setLocalCoins(response.newTotalCoins);
      spendCoins(item.price);
      setShopItems(prevItems => prevItems.map(i => i.id === itemId ? { ...i, unlocked: true } : i));

      const itemType = (item.type || 'planet').toLowerCase();
      const duplicateCount = planets.filter(p => p.type.toLowerCase() === itemType).length;

      let baseSize = 130;
      let baseX = 50;
      let baseY = 50;

      if (itemType === 'star' || itemType === 'sun') {
        baseSize = 150;
        baseX = 10;
        baseY = 50;
      } else {
        baseSize = 150;
        baseX = 30 + (duplicateCount * 15);
        baseY = 50 + (duplicateCount % 2 === 0 ? 8 : -8);
      }

      if (item.scale && Number(item.scale) > 0) baseSize = Number(item.scale);
      if (item.positionX) baseX = Number(item.positionX);
      if (item.positionY) baseY = Number(item.positionY);

      const newPlanet = {
        id: Date.now().toString(),
        name: item.name,
        type: itemType,
        size: baseSize,
        position: { x: baseX, y: baseY },
        imageUrl: item.imageUrl
      };

      setPlanets(prevPlanets => [...prevPlanets, newPlanet]);
      showToast(`${item.name} Unlocked!`);
    } catch (error) {
      console.error(error);
    }
  };

  let spentCoins = 0;
  for (const item of shopItems) {
    if (item.unlocked) spentCoins += item.price;
  }

  return (
      <div className="h-screen w-full flex flex-col bg-slate-900 overflow-hidden relative">
        {toast && (
            <div className="fixed top-6 left-1/2 transform -translate-x-1/2 z-[100] animate-in slide-in-from-top-4 fade-in duration-300">
              <div className={`flex items-center gap-3 px-5 py-3 rounded-2xl shadow-2xl border ${ toast.type === 'success' ? 'bg-green-500/20 border-green-500/30 text-green-100' : 'bg-red-500/20 border-red-500/30 text-red-100' } backdrop-blur-md`}>
                <CheckCircle className="w-5 h-5" />
                <p className="font-medium text-sm">{toast.message}</p>
              </div>
            </div>
        )}

        <div className="flex-1 overflow-hidden">
          {currentScreen === 'galaxy' && <GalaxyView coinBalance={coinBalance} planets={planets} />}
          {currentScreen === 'tasks' && <TaskDashboard tasks={tasks} onTaskToggle={handleTaskToggle} onQuickAdd={() => setIsAddTaskModalOpen(true)} onOpenChat={() => setCurrentScreen('chat')} onTaskDelete={handleTaskDelete} />}
          {currentScreen === 'habits' && <HabitTracker habits={habits} onHabitCheck={handleHabitCheck} onAddHabitClick={() => setIsAddHabitModalOpen(true)} onEditHabitClick={(h) => { setHabitToEdit(h); setIsEditHabitModalOpen(true); }} onDeleteHabit={handleDeleteHabit} />}
          {currentScreen === 'chat' && <AIChat messages={chatMessages} onSendMessage={handleSendMessage} onAddTask={() => {}} onDailyBriefing={handleDailyBriefing} isLoading={isAiLoading} />}
          {currentScreen === 'shop' && <Shop items={shopItems as any} coinBalance={coinBalance} onPurchase={handlePurchase} />}
          {currentScreen === 'statistics' && <Statistics tasks={tasks as any} habits={habits} totalCoinsEarned={coinBalance + spentCoins} currentCoins={coinBalance} />}
          {currentScreen === 'profile' && <Profile user={{ name: user?.displayName || 'Explorer', email: user?.email || '', level: 1, xp: 0, xpToNextLevel: 1000, tasksCompleted: 0, achievements: [] }} coinBalance={coinBalance} />}
        </div>

        <AddTaskModal isOpen={isAddTaskModalOpen} onClose={() => setIsAddTaskModalOpen(false)} onAdd={handleCreateNewTask} />
        <AddHabitModal isOpen={isAddHabitModalOpen} onClose={() => setIsAddHabitModalOpen(false)} onAdd={handleAddHabit} />
        <EditHabitModal isOpen={isEditHabitModalOpen} onClose={() => setIsEditHabitModalOpen(false)} habitToEdit={habitToEdit} onUpdate={handleUpdateHabit} />
        <NavigationBar currentScreen={currentScreen} onNavigate={setCurrentScreen} />
      </div>
  );
}

function AppWrapper() {
  const { user, isLoading } = useUser();
  if (isLoading) return <div className="h-screen w-full flex items-center justify-center bg-slate-950 text-white">Loading...</div>;
  if (!user) return <Login />;
  return <MainApp />;
}

export default function App() { return ( <UserProvider> <AppWrapper /> </UserProvider> ); }